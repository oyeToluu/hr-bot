package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateAuthResponse;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateLoginRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateRegisterRequest;
import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Job applicant accounts. Kept apart from /api/auth (staff) so a candidate
// can never self-register into the employee side of the app.
@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping("/register")
    public ResponseEntity<CandidateAuthResponse> register(@Valid @RequestBody CandidateRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateService.register(req));
    }

    @PostMapping("/login")
    public CandidateAuthResponse login(@Valid @RequestBody CandidateLoginRequest req) {
        return candidateService.login(req);
    }

    @GetMapping("/me")
    public Candidate me(@AuthenticationPrincipal CandidatePrincipal principal) {
        return candidateService.findById(principal.getCandidateId());
    }
}
