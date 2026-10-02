package com.hrbotproject.hr_bot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A job applicant. Kept in its own "candidates" collection, separate from
 * employees: a candidate is not on payroll, has no leave balance, and must
 * never see employee-only endpoints. Logs in with role CANDIDATE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "candidates")
public class Candidate {

    @Id
    private String id;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    private String phone;

    // BCrypt-hashed, never returned in API responses.
    @NotBlank
    @JsonIgnore
    private String password;

    @CreatedDate
    private LocalDateTime createdAt;
}
