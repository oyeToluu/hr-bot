package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateAuthResponse;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateLoginRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateRegisterRequest;
import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.repository.CandidateRepository;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public CandidateAuthResponse register(CandidateRegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        // One email = one account across staff and applicants, otherwise
        // the login lookup could not tell which account is meant.
        if (candidateRepository.existsByEmail(email) || employeeRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        Candidate candidate = candidateRepository.save(Candidate.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .phone(req.phone())
                .password(passwordEncoder.encode(req.password()))
                .build());

        String token = jwtService.generateToken(new CandidatePrincipal(candidate));
        return new CandidateAuthResponse(token, candidate);
    }

    public CandidateAuthResponse login(CandidateLoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(Locale.ROOT), req.password()));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Staff accounts must use the staff login, not the careers one.
        if (!(auth.getPrincipal() instanceof CandidatePrincipal principal)) {
            throw new BadCredentialsException("This is a staff account. Please use the staff login");
        }
        return new CandidateAuthResponse(jwtService.generateToken(principal), principal.getCandidate());
    }

    public Candidate findById(String id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + id));
    }
}
