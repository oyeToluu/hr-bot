package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// All routes here are HR-admin-only (enforced in SecurityConfig via
// /api/employees/**), and only show the HR admin's own company.
// Staff join through the company's invite link at /api/auth/register.
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/{id}")
    public Employee get(@AuthenticationPrincipal EmployeePrincipal hr, @PathVariable String id) {
        return employeeService.findById(hr.getCompanyId(), id);
    }

    @GetMapping
    public List<Employee> list(@AuthenticationPrincipal EmployeePrincipal hr) {
        return employeeService.findAll(hr.getCompanyId());
    }
}