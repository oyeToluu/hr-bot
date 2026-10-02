package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CvDownload;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.DecisionRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Applying for a job and the CV screening workflow:
 *
 *   candidate uploads CV -> text extracted -> bot scores it ->
 *     score >= job.shortlistAt        SHORTLISTED     (HR sees these first)
 *     score <  job.autoRejectBelow    AUTO_REJECTED   (HR can still reinstate)
 *     anything in between             UNDER_REVIEW
 *     CV text unreadable              UNDER_REVIEW    (never auto-rejected)
 *   -> a human in HR takes over: INTERVIEW, HIRED or REJECTED.
 */
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    // Below this many readable characters we treat the CV as unreadable
    // (typically a scanned image PDF) and send it to a human instead.
    static final int MIN_READABLE_CHARS = 150;

    private final JobApplicationRepository applicationRepository;
    private final JobPostingService jobPostingService;
    private final CandidateService candidateService;
    private final CvTextExtractor cvTextExtractor;
    private final CvScreeningService screeningService;

    public MyApplicationView apply(String jobId, String candidateId, String fileName,
                                   String contentType, byte[] fileBytes, String coverNote) {
        JobPosting job = jobPostingService.getOpen(jobId);
        Candidate candidate = candidateService.findById(candidateId);

        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new IllegalStateException("You have already applied for this job");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Please attach your CV");
        }
        if (fileBytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("CV file is too large (max 5 MB)");
        }

        ExtractedCv cv = cvTextExtractor.extract(fileName, fileBytes);
        String text = cv.text() == null ? "" : cv.text().trim();
        boolean unreadable = text.replaceAll("\\s+", "").length() < MIN_READABLE_CHARS;

        ScreeningResult result = screeningService.screen(text, job);

        JobApplication application = JobApplication.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .candidateId(candidate.getId())
                .candidateName(candidate.getFullName())
                .candidateEmail(candidate.getEmail())
                .coverNote(coverNote)
                .cvFileName(fileName)
                .cvDocumentName(cv.documentName())
                .cvContentType(contentType)
                .cvFile(fileBytes)
                .cvText(text)
                .keywordScore(result.keyword().score())
                .aiScore(result.ai() == null ? null : result.ai().score())
                .finalScore(result.finalScore())
                .matchedSkills(result.keyword().matchedSkills())
                .missingSkills(result.keyword().missingSkills())
                .bonusSkills(result.keyword().bonusSkills())
                .yearsExperienceDetected(result.keyword().yearsExperience())
                .educationMatched(result.keyword().educationMatched())
                .screeningSummary(unreadable
                        ? "Very little text could be read from this CV (it may be a scanned image). "
                          + "Sent to HR for manual review instead of being scored automatically."
                        : result.keyword().summary())
                .aiSummary(result.ai() == null ? null : result.ai().summary())
                .screenedBy(result.ai() == null ? "KEYWORD" : "KEYWORD+AI")
                .cvUnreadable(unreadable)
                .status(initialStatus(result.finalScore(), job, unreadable))
                .appliedAt(LocalDateTime.now())
                .build();

        return MyApplicationView.of(applicationRepository.save(application));
    }

    static Status initialStatus(int score, JobPosting job, boolean unreadable) {
        if (unreadable) return Status.UNDER_REVIEW;
        if (score >= job.getShortlistAt()) return Status.SHORTLISTED;
        if (score < job.getAutoRejectBelow()) return Status.AUTO_REJECTED;
        return Status.UNDER_REVIEW;
    }

    public List<MyApplicationView> mine(String candidateId) {
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId).stream()
                .map(MyApplicationView::of)
                .toList();
    }

    /** HR: every application for a job, best score first. */
    public List<JobApplication> forJob(String jobId) {
        jobPostingService.findById(jobId); // 404 if the job doesn't exist
        return applicationRepository.findByJobIdOrderByFinalScoreDesc(jobId);
    }

    public JobApplication findById(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Application not found: " + id));
    }

    public CvDownload downloadCv(String id) {
        JobApplication a = findById(id);
        // Browser-supplied content type; only trust a plain "type/subtype".
        String type = a.getCvContentType() != null && a.getCvContentType().matches("[\\w.+-]+/[\\w.+-]+")
                ? a.getCvContentType()
                : "application/octet-stream";
        return new CvDownload(a.getCvFileName(), type, a.getCvFile());
    }

    /** The human takes over: HR moves the application on, or reinstates an auto-reject. */
    public JobApplication decide(String id, DecisionRequest req, String hrEmployeeId) {
        JobApplication a = findById(id);

        Status next = switch (req.decision()) {
            case INTERVIEW -> Status.INTERVIEW;
            case HIRED -> Status.HIRED;
            case REJECTED -> Status.REJECTED;
            case REINSTATE -> {
                if (a.getStatus() != Status.AUTO_REJECTED) {
                    throw new IllegalArgumentException("Only auto-rejected applications can be reinstated");
                }
                yield Status.UNDER_REVIEW;
            }
        };
        if (a.getStatus() == Status.HIRED || a.getStatus() == Status.REJECTED) {
            throw new IllegalStateException("This application already has a final decision");
        }

        a.setStatus(next);
        a.setHrNote(req.note());
        a.setDecidedBy(hrEmployeeId);
        a.setDecidedAt(LocalDateTime.now());
        return applicationRepository.save(a);
    }
}
