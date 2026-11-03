package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateLoginRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateRegisterRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.AuthResult;
import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.service.AccountSecurityService;
import com.hrbotproject.hr_bot.service.CandidateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Job applicant accounts. Kept apart from /api/auth (staff) so a candidate
// can never self-register into the employee side of the app. The emailed-code
// steps (verify, resend, forgot / reset password) are shared under /api/auth.
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final AccountSecurityService accounts;
    private final CandidateService candidateService;

    public CandidateController(AccountSecurityService accounts, CandidateService candidateService) {
        this.accounts = accounts;
        this.candidateService = candidateService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResult> register(@Valid @RequestBody CandidateRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.registerCandidate(req));
    }

    @PostMapping("/login")
    public AuthResult login(@Valid @RequestBody CandidateLoginRequest req) {
        return accounts.loginCandidate(req);
    }

    @GetMapping("/me")
    public Candidate me(@AuthenticationPrincipal CandidatePrincipal principal) {
        return candidateService.findById(principal.getCandidateId());
    }
}