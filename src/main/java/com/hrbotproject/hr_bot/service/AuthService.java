package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.AuthDtos.AuthResponse;
import com.hrbotproject.hr_bot.dto.AuthDtos.LoginRequest;
import com.hrbotproject.hr_bot.dto.AuthDtos.RegisterRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.CandidateRepository;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Staff account logic: register, log in, and "who am I".
 * AuthController only receives the HTTP request and calls this service.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        // One email = one account across staff and job applicants.
        if (employeeRepository.existsByEmail(req.email()) || candidateRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        Employee employee = Employee.builder()
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .department(req.department() == null ? "Unassigned" : req.department())
                .role(Employee.Role.EMPLOYEE) // always EMPLOYEE on self-registration
                .build();
        employeeRepository.save(employee);

        String token = jwtService.generateToken(new EmployeePrincipal(employee));
        return new AuthResponse(token, employee);
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Candidate accounts must use the careers login, not the staff one.
        if (!(auth.getPrincipal() instanceof EmployeePrincipal principal)) {
            throw new BadCredentialsException("This is a candidate account. Please log in on the careers page");
        }
        return new AuthResponse(jwtService.generateToken(principal), principal.getEmployee());
    }

    public Employee me(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));
    }
}