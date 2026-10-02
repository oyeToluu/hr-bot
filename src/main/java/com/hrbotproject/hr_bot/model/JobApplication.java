package com.hrbotproject.hr_bot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One candidate's application to one job, plus the screening bot's verdict.
 * The bot sets the first status (SHORTLISTED / UNDER_REVIEW / AUTO_REJECTED);
 * after that only HR moves it (INTERVIEW / HIRED / REJECTED, or reinstates
 * an AUTO_REJECTED application back to UNDER_REVIEW).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "job_applications")
@CompoundIndex(name = "one_application_per_job", def = "{'jobId': 1, 'candidateId': 1}", unique = true)
public class JobApplication {

    public enum Status { SHORTLISTED, UNDER_REVIEW, AUTO_REJECTED, INTERVIEW, HIRED, REJECTED }

    @Id
    private String id;

    private String jobId;
    private String jobTitle;

    private String candidateId;
    private String candidateName;
    private String candidateEmail;

    private String coverNote;

    // ---- the uploaded CV ----
    private String cvFileName;        // what the candidate uploaded (may be a .zip)
    private String cvDocumentName;    // the file actually read (the PDF/DOCX inside a zip)
    private String cvContentType;

    @JsonIgnore
    private byte[] cvFile;            // original upload, for HR to download

    @JsonIgnore
    private String cvText;            // extracted text the bot scored

    // ---- the screening bot's verdict ----
    private int keywordScore;         // 0-100, rule-based, always computed
    private Integer aiScore;          // 0-100 from the AI model, null if AI is off or failed
    private int finalScore;           // what the status is based on

    @Builder.Default
    private List<String> matchedSkills = new ArrayList<>();
    @Builder.Default
    private List<String> missingSkills = new ArrayList<>();
    @Builder.Default
    private List<String> bonusSkills = new ArrayList<>();

    private int yearsExperienceDetected;
    private boolean educationMatched;

    private String screeningSummary;  // plain-English explanation for HR
    private String aiSummary;         // the AI model's own explanation, if used
    private String screenedBy;        // "KEYWORD" or "KEYWORD+AI"

    // True when too little text could be read (e.g. a scanned image PDF).
    // These go to UNDER_REVIEW and are never auto-rejected.
    private boolean cvUnreadable;

    private Status status;

    // ---- HR's decision ----
    private String hrNote;
    private String decidedBy;
    private LocalDateTime decidedAt;

    private LocalDateTime appliedAt;
}
