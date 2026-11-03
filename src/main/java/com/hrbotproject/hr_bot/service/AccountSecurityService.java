package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.AuthDtos.LoginRequest;
import com.hrbotproject.hr_bot.dto.AuthDtos.RegisterRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateLoginRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CandidateRegisterRequest;
import com.hrbotproject.hr_bot.dto.SecurityDtos.AuthResult;
import com.hrbotproject.hr_bot.dto.SecurityDtos.MessageResponse;
import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.model.EmailCode.Purpose;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.CandidateRepository;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.security.DeviceTrust;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sign-up, login and password reset, with emailed codes:
 *  - new accounts confirm their email with a code before they can log in
 *  - logging in on a new device needs a code (the device is then remembered for 30 days)
 *  - "Forgot password" emails a code that lets you choose a new password
 * Works for both staff (employees / HR admins) and job applicants (candidates).
 */
@Service
public class AccountSecurityService {

    static final String SENT_IF_EXISTS =
            "If an account exists for that email, we've sent a code to it. Check your inbox and spam folder.";

    private final EmployeeRepository employeeRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailCodeService codes;
    private final DeviceTrust deviceTrust;
    private final boolean codesEnabled;
    private final Set<String> exemptEmails;

    public AccountSecurityService(
            EmployeeRepository employeeRepository,
            CandidateRepository candidateRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmailCodeService codes,
            DeviceTrust deviceTrust,
            @Value("${app.auth.email-codes-enabled:true}") boolean codesEnabled,
            @Value("${app.auth.code-exempt-emails:}") String exemptEmails
    ) {
        this.employeeRepository = employeeRepository;
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.codes = codes;
        this.deviceTrust = deviceTrust;
        this.codesEnabled = codesEnabled;
        this.exemptEmails = Arrays.stream(exemptEmails.split(","))
                .map(AccountSecurityService::normalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    // ---------- sign up ----------

    public AuthResult registerEmployee(RegisterRequest req) {
        String email = normalize(req.email());
        makeRoomFor(email);
        boolean needsCode = codesRequired(email);

        Employee employee = employeeRepository.save(Employee.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .department(req.department() == null || req.department().isBlank() ? "Unassigned" : req.department().trim())
                .role(Employee.Role.EMPLOYEE) // always EMPLOYEE on self-registration
                .emailVerified(!needsCode)
                .build());

        if (!needsCode) return loggedIn(employee, null);
        try {
            codes.issue(email, Purpose.SIGNUP, employee.getFullName(), false);
        } catch (IllegalStateException e) {
            employeeRepository.delete(employee); // couldn't email the code: let them try again cleanly
            throw e;
        }
        return AuthResult.codeSent(email, Purpose.SIGNUP);
    }

    public AuthResult registerCandidate(CandidateRegisterRequest req) {
        String email = normalize(req.email());
        makeRoomFor(email);
        boolean needsCode = codesRequired(email);

        Candidate candidate = candidateRepository.save(Candidate.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .phone(req.phone())
                .password(passwordEncoder.encode(req.password()))
                .emailVerified(!needsCode)
                .build());

        if (!needsCode) return loggedIn(candidate, null);
        try {
            codes.issue(email, Purpose.SIGNUP, candidate.getFullName(), false);
        } catch (IllegalStateException e) {
            candidateRepository.delete(candidate);
            throw e;
        }
        return AuthResult.codeSent(email, Purpose.SIGNUP);
    }

    /**
     * One email = one account across staff and applicants. An account that never
     * confirmed its email doesn't block the real owner from signing up.
     */
    private void makeRoomFor(String email) {
        Optional<Employee> employee = findEmployee(email);
        if (employee.isPresent()) {
            if (Boolean.FALSE.equals(employee.get().getEmailVerified())) employeeRepository.delete(employee.get());
            else throw new IllegalArgumentException("An account with this email already exists");
        }
        Optional<Candidate> candidate = findCandidate(email);
        if (candidate.isPresent()) {
            if (Boolean.FALSE.equals(candidate.get().getEmailVerified())) candidateRepository.delete(candidate.get());
            else throw new IllegalArgumentException("An account with this email already exists");
        }
    }

    // ---------- log in ----------

    public AuthResult loginEmployee(LoginRequest req) {
        Authentication auth = checkPassword(req.email(), req.password());
        if (!(auth.getPrincipal() instanceof EmployeePrincipal principal)) {
            throw new BadCredentialsException("This is a candidate account. Please log in on the careers page");
        }
        Employee e = principal.getEmployee();
        return afterPassword(e.getEmail(), e.getFullName(), e.getPassword(), e.getEmailVerified(),
                req.deviceToken(), () -> loggedIn(e, null));
    }

    public AuthResult loginCandidate(CandidateLoginRequest req) {
        Authentication auth = checkPassword(req.email(), req.password());
        if (!(auth.getPrincipal() instanceof CandidatePrincipal principal)) {
            throw new BadCredentialsException("This is a staff account. Please use the staff login");
        }
        Candidate c = principal.getCandidate();
        return afterPassword(c.getEmail(), c.getFullName(), c.getPassword(), c.getEmailVerified(),
                req.deviceToken(), () -> loggedIn(c, null));
    }

    private Authentication checkPassword(String email, String password) {
        String trimmed = email.trim();
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(trimmed, password));
        } catch (AuthenticationException first) {
            // Older accounts may have been saved with capital letters in the email
            String lower = normalize(trimmed);
            if (!lower.equals(trimmed)) {
                try {
                    return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(lower, password));
                } catch (AuthenticationException ignored) {
                    // fall through
                }
            }
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    private AuthResult afterPassword(String email, String name, String passwordHash, Boolean verified,
                                     String deviceToken, java.util.function.Supplier<AuthResult> success) {
        if (!codesRequired(email)) return success.get();
        String key = normalize(email);
        if (Boolean.FALSE.equals(verified)) {
            codes.issue(key, Purpose.SIGNUP, name, false);
            return AuthResult.codeSent(key, Purpose.SIGNUP);
        }
        if (deviceTrust.isTrusted(deviceToken, key, passwordHash)) return success.get();
        codes.issue(key, Purpose.LOGIN, name, false);
        return AuthResult.codeSent(key, Purpose.LOGIN);
    }

    // ---------- codes ----------

    /** Checks a sign-up or new-device code and logs the person in. */
    public AuthResult verifyCode(String rawEmail, Purpose purpose, String code) {
        if (purpose == Purpose.RESET) {
            throw new IllegalArgumentException("Use the reset password form for this code");
        }
        String email = normalize(rawEmail);
        codes.verify(email, purpose, code);

        Optional<Employee> employee = findEmployee(email);
        if (employee.isPresent()) {
            Employee e = employee.get();
            if (!Boolean.TRUE.equals(e.getEmailVerified())) {
                e.setEmailVerified(true);
                e = employeeRepository.save(e);
            }
            return loggedIn(e, deviceTrust.issue(email, e.getPassword()));
        }
        Optional<Candidate> candidate = findCandidate(email);
        if (candidate.isPresent()) {
            Candidate c = candidate.get();
            if (!Boolean.TRUE.equals(c.getEmailVerified())) {
                c.setEmailVerified(true);
                c = candidateRepository.save(c);
            }
            return loggedIn(c, deviceTrust.issue(email, c.getPassword()));
        }
        throw new NoSuchElementException("Account not found");
    }

    /** "Send me a new code". Only works right after a login or sign-up asked for a code. */
    public MessageResponse resendCode(String rawEmail, Purpose purpose) {
        String email = normalize(rawEmail);
        if (purpose == Purpose.RESET) {
            forgotPassword(email);
            return new MessageResponse(SENT_IF_EXISTS);
        }
        if (!codes.hasRecent(email, purpose)) {
            throw new IllegalArgumentException("Please log in again to get a new code.");
        }
        codes.issue(email, purpose, nameOf(email), true);
        return new MessageResponse("We've sent you a new code.");
    }

    // ---------- forgot password ----------

    /** Emails a reset code. Gives the same answer whether or not the account exists. */
    public MessageResponse forgotPassword(String rawEmail) {
        String email = normalize(rawEmail);
        if (findEmployee(email).isPresent() || findCandidate(email).isPresent()) {
            codes.issue(email, Purpose.RESET, nameOf(email), false);
        }
        return new MessageResponse(SENT_IF_EXISTS);
    }

    /** Checks the reset code and saves the new password. This browser is then trusted. */
    public AuthResult resetPassword(String rawEmail, String code, String newPassword) {
        String email = normalize(rawEmail);
        codes.verify(email, Purpose.RESET, code);
        String hash = passwordEncoder.encode(newPassword);

        Optional<Employee> employee = findEmployee(email);
        if (employee.isPresent()) {
            Employee e = employee.get();
            e.setPassword(hash);
            e.setEmailVerified(true); // they just proved they own the email
            employeeRepository.save(e);
            return new AuthResult("PASSWORD_RESET", Purpose.RESET, email, null, null, null, deviceTrust.issue(email, hash));
        }
        Optional<Candidate> candidate = findCandidate(email);
        if (candidate.isPresent()) {
            Candidate c = candidate.get();
            c.setPassword(hash);
            c.setEmailVerified(true);
            candidateRepository.save(c);
            return new AuthResult("PASSWORD_RESET", Purpose.RESET, email, null, null, null, deviceTrust.issue(email, hash));
        }
        throw new NoSuchElementException("Account not found");
    }

    // ---------- helpers ----------

    private boolean codesRequired(String email) {
        return codesEnabled && !exemptEmails.contains(normalize(email));
    }

    private AuthResult loggedIn(Employee e, String deviceToken) {
        return new AuthResult("OK", null, e.getEmail(), jwtService.generateToken(new EmployeePrincipal(e)),
                e, null, deviceToken);
    }

    private AuthResult loggedIn(Candidate c, String deviceToken) {
        return new AuthResult("OK", null, c.getEmail(), jwtService.generateToken(new CandidatePrincipal(c)),
                null, c, deviceToken);
    }

    private Optional<Employee> findEmployee(String email) {
        return employeeRepository.findByEmail(email);
    }

    private Optional<Candidate> findCandidate(String email) {
        return candidateRepository.findByEmail(email);
    }

    private String nameOf(String email) {
        return findEmployee(email).map(Employee::getFullName)
                .or(() -> findCandidate(email).map(Candidate::getFullName))
                .orElse("");
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}