package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.AiScore;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.Decision;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.DecisionRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.KeywordScore;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.MyApplicationView;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.ScreeningResult;
import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.model.JobApplication;
import com.hrbotproject.hr_bot.model.JobApplication.Status;
import com.hrbotproject.hr_bot.model.JobPosting;
import com.hrbotproject.hr_bot.repository.JobApplicationRepository;
import com.hrbotproject.hr_bot.screening.CvScreeningService;
import com.hrbotproject.hr_bot.screening.CvTextExtractor;
import com.hrbotproject.hr_bot.screening.CvTextExtractor.ExtractedCv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock private JobApplicationRepository applicationRepository;
    @Mock private JobPostingService jobPostingService;
    @Mock private CandidateService candidateService;
    @Mock private CvTextExtractor cvTextExtractor;
    @Mock private CvScreeningService screeningService;

    private JobApplicationService service;

    private final JobPosting job = JobPosting.builder()
            .id("job-1").title("Junior Backend Developer")
            .autoRejectBelow(40).shortlistAt(70)
            .build();

    private final Candidate candidate = Candidate.builder()
            .id("cand-1").fullName("Chidi Eze").email("chidi@example.com")
            .build();

    // Long enough to count as readable (over MIN_READABLE_CHARS)
    private static final String READABLE_CV = "Java Spring Boot MongoDB developer. ".repeat(10);

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(applicationRepository, jobPostingService,
                candidateService, cvTextExtractor, screeningService);
    }

    private void givenCvScores(int finalScore) {
        givenCvText(READABLE_CV);
        KeywordScore kw = new KeywordScore(finalScore, List.of("Java"), List.of("Git"), List.of(), 2, true, "summary");
        when(screeningService.screen(anyString(), any())).thenReturn(new ScreeningResult(kw, null, finalScore));
    }

    private void givenCvText(String text) {
        when(jobPostingService.getOpen("job-1")).thenReturn(job);
        when(candidateService.findById("cand-1")).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId("job-1", "cand-1")).thenReturn(false);
        when(cvTextExtractor.extract(anyString(), any())).thenReturn(new ExtractedCv("cv.pdf", text));
        lenient().when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MyApplicationView applyWithCv() {
        return service.apply("job-1", "cand-1", "cv.pdf", "application/pdf", new byte[]{1, 2, 3}, null);
    }

    @Test
    @DisplayName("A high score is shortlisted for HR")
    void highScore_isShortlisted() {
        givenCvScores(85);

        assertThat(applyWithCv().status()).isEqualTo(Status.SHORTLISTED);
    }

    @Test
    @DisplayName("A middling score goes to HR for review")
    void middleScore_isUnderReview() {
        givenCvScores(55);

        assertThat(applyWithCv().status()).isEqualTo(Status.UNDER_REVIEW);
    }

    @Test
    @DisplayName("A score below the job's threshold is auto-rejected")
    void lowScore_isAutoRejected() {
        givenCvScores(25);

        assertThat(applyWithCv().status()).isEqualTo(Status.AUTO_REJECTED);
    }

    @Test
    @DisplayName("An unreadable CV (e.g. scanned image) is never auto-rejected")
    void unreadableCv_goesToHumanReview() {
        givenCvText("   ");
        KeywordScore kw = new KeywordScore(5, List.of(), List.of("Java"), List.of(), 0, false, "summary");
        when(screeningService.screen(anyString(), any())).thenReturn(new ScreeningResult(kw, null, 5));

        MyApplicationView view = applyWithCv();

        assertThat(view.status()).isEqualTo(Status.UNDER_REVIEW);
        assertThat(view.cvUnreadable()).isTrue();
    }

    @Test
    @DisplayName("Saves both scores and marks the AI as used when it answered")
    void savesKeywordAndAiScores() {
        givenCvText(READABLE_CV);
        KeywordScore kw = new KeywordScore(60, List.of("Java"), List.of(), List.of(), 3, true, "kw summary");
        when(screeningService.screen(anyString(), any()))
                .thenReturn(new ScreeningResult(kw, new AiScore(80, "ai summary"), 70));

        applyWithCv();

        ArgumentCaptor<JobApplication> saved = ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationRepository).save(saved.capture());
        assertThat(saved.getValue().getKeywordScore()).isEqualTo(60);
        assertThat(saved.getValue().getAiScore()).isEqualTo(80);
        assertThat(saved.getValue().getFinalScore()).isEqualTo(70);
        assertThat(saved.getValue().getScreenedBy()).isEqualTo("KEYWORD+AI");
        assertThat(saved.getValue().getStatus()).isEqualTo(Status.SHORTLISTED);
    }

    @Test
    @DisplayName("A candidate cannot apply twice for the same job")
    void duplicateApplication_isRejected() {
        when(jobPostingService.getOpen("job-1")).thenReturn(job);
        when(candidateService.findById("cand-1")).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId("job-1", "cand-1")).thenReturn(true);

        assertThatThrownBy(this::applyWithCv)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already applied");
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("HR can reinstate an auto-rejected application")
    void hrCanReinstateAutoReject() {
        JobApplication app = JobApplication.builder().id("app-1").status(Status.AUTO_REJECTED).build();
        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        JobApplication result = service.decide("app-1",
                new DecisionRequest(Decision.REINSTATE, "Strong portfolio, worth a look"), "hr-1");

        assertThat(result.getStatus()).isEqualTo(Status.UNDER_REVIEW);
        assertThat(result.getDecidedBy()).isEqualTo("hr-1");
        assertThat(result.getHrNote()).isEqualTo("Strong portfolio, worth a look");
    }

    @Test
    @DisplayName("A final decision (hired/rejected) cannot be changed")
    void finalDecision_isLocked() {
        JobApplication app = JobApplication.builder().id("app-1").status(Status.HIRED).build();
        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.decide("app-1", new DecisionRequest(Decision.REJECTED, null), "hr-1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
