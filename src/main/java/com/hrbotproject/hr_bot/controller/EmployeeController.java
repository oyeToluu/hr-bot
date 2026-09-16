package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// All routes here are HR-admin-only (enforced in SecurityConfig via
// /api/employees/**). Employee creation intentionally lives at
// /api/auth/register instead, since that's the only path that sets a
// password - an employee created here would never be able to log in.
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/{id}")
    public Employee get(@PathVariable String id) {
        return employeeService.findById(id);
    }

    @GetMapping
    public List<Employee> list() {
        return employeeService.findAll();
    }
}
