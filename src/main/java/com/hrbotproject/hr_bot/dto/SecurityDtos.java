package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.model.EmailCode;
import com.hrbotproject.hr_bot.model.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SecurityDtos {

    /** At least 8 characters, with a letter, a number and a special character. */
    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$";
    public static final String PASSWORD_HINT =
            "Use at least 8 characters, with a letter, a number and a special character (like @ # ! ?)";

    /**
     * What login, sign-up and code checks send back.
     * status "OK": logged in (token + employee or candidate).
     * status "CODE_SENT": a code was emailed; purpose says why (SIGNUP or LOGIN).
     * status "PASSWORD_RESET": password changed; deviceToken lets this browser skip the next code.
     */
    public record AuthResult(
            String status,
            EmailCode.Purpose purpose,
            String email,
            String token,
            Employee employee,
            Candidate candidate,
            String deviceToken
    ) {
        public static AuthResult codeSent(String email, EmailCode.Purpose purpose) {
            return new AuthResult("CODE_SENT", purpose, email, null, null, null, null);
        }
    }

    public record VerifyCodeRequest(
            @NotBlank @Email String email,
            @NotNull EmailCode.Purpose purpose,
            @NotBlank @Pattern(regexp = "\\s*\\d{6}\\s*", message = "the code is 6 digits") String code
    ) {}

    public record ResendCodeRequest(
            @NotBlank @Email String email,
            @NotNull EmailCode.Purpose purpose
    ) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\s*\\d{6}\\s*", message = "the code is 6 digits") String code,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_HINT) String newPassword
    ) {}

    public record MessageResponse(String message) {}
}