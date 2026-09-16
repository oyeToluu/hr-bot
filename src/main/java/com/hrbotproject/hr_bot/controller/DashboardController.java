package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.DashboardDtos.AdminDashboard;
import com.hrbotproject.hr_bot.dto.DashboardDtos.EmployeeDashboard;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    public EmployeeDashboard myDashboard(@AuthenticationPrincipal EmployeePrincipal principal) {
        return dashboardService.myDashboard(principal.getEmployeeId());
    }

    // HR only (enforced in SecurityConfig)
    @GetMapping("/admin")
    public AdminDashboard adminDashboard() {
        return dashboardService.adminDashboard();
    }
}
