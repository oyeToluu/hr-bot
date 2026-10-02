package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.model.JobApplication;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class RecruitmentDtos {

    // ---- candidate accounts ----

    public record CandidateRegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "password must be at least 6 characters") String password,
            String phone
    ) {}

    public record CandidateLoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record CandidateAuthResponse(String token, Candidate candidate) {}

    // ---- job postings (HR) ----

    public record JobRequest(
            @NotBlank String title,
            String department,
            String location,
            String description,
            List<String> requiredSkills,
            List<String> niceToHaveSkills,
            @Min(0) @Max(40) Integer minYearsExperience,
            List<String> educationKeywords,
            @Min(0) @Max(100) Integer autoRejectBelow,
            @Min(0) @Max(100) Integer shortlistAt
    ) {}

    public record JobSummary(
            String id,
            String title,
            String department,
            String location,
            boolean open,
            long applications,
            LocalDateTime createdAt
    ) {}

    // ---- HR decision on an application ----

    public enum Decision { INTERVIEW, HIRED, REJECTED, REINSTATE }

    public record DecisionRequest(@NotNull Decision decision, String note) {}

    // ---- what a candidate sees about their own application ----
    // No HR notes, no internal thresholds: just status and skill feedback.

    public record MyApplicationView(
            String id,
            String jobId,
            String jobTitle,
            JobApplication.Status status,
            int score,
            List<String> matchedSkills,
            List<String> missingSkills,
            boolean cvUnreadable,
            LocalDateTime appliedAt
    ) {
        public static MyApplicationView of(JobApplication a) {
            return new MyApplicationView(a.getId(), a.getJobId(), a.getJobTitle(), a.getStatus(),
                    a.getFinalScore(), a.getMatchedSkills(), a.getMissingSkills(),
                    a.isCvUnreadable(), a.getAppliedAt());
        }
    }

    // ---- result of the screening bot ----

    public record KeywordScore(
            int score,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> bonusSkills,
            int yearsExperience,
            boolean educationMatched,
            String summary
    ) {}

    public record AiScore(int score, String summary) {}

    public record ScreeningResult(
            KeywordScore keyword,
            AiScore ai,          // null when the AI model is off or failed
            int finalScore
    ) {}

    // ---- downloaded CV file for HR ----

    public record CvDownload(String fileName, String contentType, byte[] bytes) {}
}
