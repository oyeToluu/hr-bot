package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = SecurityDtos.PASSWORD_RULE, message = SecurityDtos.PASSWORD_HINT) String password,
            String department,
            @NotBlank(message = "Please use the invite link from your HR team") String inviteCode
            // No "role" field here on purpose: self-registration should never
            // be able to grant itself HR_ADMIN. HR admin accounts are seeded
            // or promoted by an existing HR_ADMIN, not chosen at signup.
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String deviceToken // proves this browser entered a code recently (optional)
    ) {}

    public record AuthResponse(String token, Employee employee) {}
}