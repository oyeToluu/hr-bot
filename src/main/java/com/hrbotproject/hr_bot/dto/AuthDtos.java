package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "password must be at least 6 characters") String password,
            String department
            // No "role" field here on purpose: self-registration should never
            // be able to grant itself HR_ADMIN. HR admin accounts are seeded
            // or promoted by an existing HR_ADMIN, not chosen at signup.
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String token, Employee employee) {}
}
