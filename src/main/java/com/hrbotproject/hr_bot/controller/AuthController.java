package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.AuthDtos.LoginRequest;
import com.hrbotproject.hr_bot.dto.AuthDtos.RegisterRequest;
import com.hrbotproject.hr_bot.dto.CompanyDtos.RegisterCompanyRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.AuthResult;
import com.hrbotproject.hr_bot.dto.SecurityDtos.CheckResetCodeRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.ForgotPasswordRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.MessageResponse;
import com.hrbotproject.hr_bot.dto.SecurityDtos.ResendCodeRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.ResetPasswordRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.VerifyCodeRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.AccountSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Staff sign-up and login, plus the emailed-code steps shared by staff and
 * job applicants (verify code, resend code, forgot / reset password).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountSecurityService accounts;

    public AuthController(AccountSecurityService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResult> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.registerEmployee(req));
    }

        // A business signing up: the company and its first HR admin
    @PostMapping("/register-company")
    public ResponseEntity<AuthResult> registerCompany(@Valid @RequestBody RegisterCompanyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.registerCompany(req));
    }

    @PostMapping("/login")
    public AuthResult login(@Valid @RequestBody LoginRequest req) {
        return accounts.loginEmployee(req);
    }

    @PostMapping("/verify-code")
    public AuthResult verifyCode(@Valid @RequestBody VerifyCodeRequest req) {
        return accounts.verifyCode(req.email(), req.purpose(), req.code());
    }

    @PostMapping("/resend-code")
    public MessageResponse resendCode(@Valid @RequestBody ResendCodeRequest req) {
        return accounts.resendCode(req.email(), req.purpose());
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return accounts.forgotPassword(req.email());
    }

    @PostMapping("/check-reset-code")
    public MessageResponse checkResetCode(@Valid @RequestBody CheckResetCodeRequest req) {
        return accounts.checkResetCode(req.email(), req.code());
    }

    @PostMapping("/reset-password")
    public AuthResult resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return accounts.resetPassword(req.email(), req.code(), req.newPassword());
    }

    @GetMapping("/me")
    public Employee me(@AuthenticationPrincipal EmployeePrincipal principal) {
        return principal.getEmployee();
    }
}