package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

// Employee creation lives in AuthController (/api/auth/register), since
// that's the only path that sets a password. This service is now
// read-only, backing the HR-admin employee list/detail endpoints.
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public Employee findById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + id));
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
}
