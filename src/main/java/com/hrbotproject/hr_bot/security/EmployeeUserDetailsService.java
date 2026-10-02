package com.hrbotproject.hr_bot.security;

import com.hrbotproject.hr_bot.repository.CandidateRepository;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Looks a login email up in employees first, then in candidates. One lookup
 * service means one AuthenticationManager and one JWT filter serve both
 * kinds of user; the principal type (EmployeePrincipal / CandidatePrincipal)
 * and its role decide what each can reach.
 */
@Service
@RequiredArgsConstructor
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final CandidateRepository candidateRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return employeeRepository.findByEmail(email)
                .<UserDetails>map(EmployeePrincipal::new)
                .or(() -> candidateRepository.findByEmail(email).map(CandidatePrincipal::new))
                .orElseThrow(() -> new UsernameNotFoundException("No account with email: " + email));
    }
}
