package com.hrbotproject.hr_bot.security;

import com.hrbotproject.hr_bot.model.Candidate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in job applicant. Has the single authority ROLE_CANDIDATE, so
 * SecurityConfig can keep candidates on the careers endpoints and away from
 * everything employees use.
 */
public class CandidatePrincipal implements UserDetails {

    private final Candidate candidate;

    public CandidatePrincipal(Candidate candidate) {
        this.candidate = candidate;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public String getCandidateId() {
        return candidate.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"));
    }

    @Override
    public String getPassword() {
        return candidate.getPassword();
    }

    @Override
    public String getUsername() {
        return candidate.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
