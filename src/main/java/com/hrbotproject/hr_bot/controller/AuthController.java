package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.AuthDtos.AuthResponse;
import com.hrbotproject.hr_bot.dto.AuthDtos.LoginRequest;
import com.hrbotproject.hr_bot.dto.AuthDtos.RegisterRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (employeeRepository.existsByEmail(req.email())) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, employee));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Employee employee = employeeRepository.findByEmail(req.email())
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));

        String token = jwtService.generateToken(new EmployeePrincipal(employee));
        return new AuthResponse(token, employee);
    }

    @GetMapping("/me")
    public Employee me(@AuthenticationPrincipal EmployeePrincipal principal) {
        return principal.getEmployee();
    }
}
