package com.hrbotproject.hr_bot.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An open position. The skill lists and thresholds are what the CV
 * screening bot scores applications against.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "job_postings")
public class JobPosting {

    @Id
    private String id;

    @NotBlank
    private String title;

    private String department;

    private String location;

    private String description;

    // Must-have skills: worth 60 of the 100 keyword-score points.
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    // Bonus skills: worth 15 points.
    @Builder.Default
    private List<String> niceToHaveSkills = new ArrayList<>();

    // Worth 15 points; 0 means "no minimum" (full marks).
    @Builder.Default
    private int minYearsExperience = 0;

    // e.g. "bsc", "computer science", "hnd". Worth 10 points; empty = full marks.
    @Builder.Default
    private List<String> educationKeywords = new ArrayList<>();

    // Final score below this -> AUTO_REJECTED (HR can still reinstate).
    @Builder.Default
    private int autoRejectBelow = 40;

    // Final score at or above this -> SHORTLISTED. In between -> UNDER_REVIEW.
    @Builder.Default
    private int shortlistAt = 70;

    @Builder.Default
    private boolean open = true;

    private String createdBy;

    @CreatedDate
    private LocalDateTime createdAt;
}
