package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.OnboardingDtos.AskRequest;
import com.hrbotproject.hr_bot.dto.OnboardingDtos.AskResponse;
import com.hrbotproject.hr_bot.dto.OnboardingDtos.CreateFaqRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.OnboardingFaq;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/ask")
    public AskResponse ask(@AuthenticationPrincipal EmployeePrincipal principal, @Valid @RequestBody AskRequest req) {
        return onboardingService.ask(principal.getCompanyId(), req.message());
    }

    @GetMapping("/faqs")
    public List<OnboardingFaq> listFaqs(@AuthenticationPrincipal EmployeePrincipal principal) {
        return onboardingService.listFaqs(principal.getCompanyId());
    }

    // HR only (enforced in SecurityConfig). The FAQ goes into HR's own company.
    @PostMapping("/faqs")
    public ResponseEntity<OnboardingFaq> createFaq(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @Valid @RequestBody CreateFaqRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.createFaq(principal.getCompanyId(), req));
    }

    @PatchMapping("/complete")
    public Employee markComplete(@AuthenticationPrincipal EmployeePrincipal principal) {
        return onboardingService.markOnboardingComplete(principal.getEmployeeId());
    }
}