package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.CompanyDtos.CompanyView;
import com.hrbotproject.hr_bot.dto.CompanyDtos.InviteRequest;
import com.hrbotproject.hr_bot.dto.CompanyDtos.PublicCompany;
import com.hrbotproject.hr_bot.dto.SecurityDtos.MessageResponse;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Who can call what is set in SecurityConfig:
//   GET  /api/company                       any logged-in staff (their own company)
//   POST /api/company/invite, /join-link/*  HR_ADMIN
//   GET  /api/companies/**                  anyone (join page, careers page)
@RestController
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/api/company")
    public CompanyView mine(@AuthenticationPrincipal EmployeePrincipal me) {
        return companyService.view(me.getEmployee());
    }

    @PostMapping("/api/company/invite")
    public MessageResponse invite(@AuthenticationPrincipal EmployeePrincipal hr, @Valid @RequestBody InviteRequest req) {
        return companyService.invite(hr.getEmployee(), req.email());
    }

    @PostMapping("/api/company/join-link/reset")
    public CompanyView resetJoinLink(@AuthenticationPrincipal EmployeePrincipal hr) {
        return companyService.resetJoinLink(hr.getEmployee());
    }

    // The "Join Zenith Foods Ltd" page checks the link before showing the form
    @GetMapping("/api/companies/join/{code}")
    public PublicCompany byJoinCode(@PathVariable String code) {
        return PublicCompany.of(companyService.byJoinCode(code));
    }

    // A company's own careers page
    @GetMapping("/api/companies/{slug}")
    public PublicCompany bySlug(@PathVariable String slug) {
        return PublicCompany.of(companyService.bySlug(slug));
    }
}