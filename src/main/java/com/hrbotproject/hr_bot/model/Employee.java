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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    private String department;

    // BCrypt-hashed, never plaintext. @JsonIgnore keeps it out of every API
    // response (GET /api/employees would otherwise leak the hash).
    @NotBlank
    @JsonIgnore
    private String password;

    public enum Role { EMPLOYEE, HR_ADMIN }

    @Builder.Default
    private Role role = Role.EMPLOYEE;

    @Builder.Default
    private LocalDate dateStarted = LocalDate.now();

    @Builder.Default
    private double leaveBalance = 20.0;

    @Builder.Default
    private boolean onboardingComplete = false;

    @CreatedDate
    private LocalDateTime createdAt;
}
