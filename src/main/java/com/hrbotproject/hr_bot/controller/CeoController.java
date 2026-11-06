package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.OwnerDtos.OwnerCheck;
import com.hrbotproject.hr_bot.dto.OwnerDtos.OwnerOverview;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.OwnerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The HR Bots owner's own pages. Every staff member may ask "am I the owner?";
// only the owner gets the company list (checked in OwnerService).
@RestController
@RequestMapping("/api/owner")
public class CeoController {

    private final OwnerService ownerService;

    public CeoController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/check")
    public OwnerCheck check(@AuthenticationPrincipal EmployeePrincipal me) {
        return new OwnerCheck(ownerService.isOwner(me.getEmployee()));
    }

    @GetMapping("/companies")
    public OwnerOverview companies(@AuthenticationPrincipal EmployeePrincipal me) {
        return ownerService.overview(me.getEmployee());
    }
}