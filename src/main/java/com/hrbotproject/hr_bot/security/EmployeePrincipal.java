package com.hrbotproject.hr_bot.security;

import com.hrbotproject.hr_bot.model.Employee;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our Employee document to what Spring Security expects.
 * Authorities are "ROLE_EMPLOYEE" / "ROLE_HR_ADMIN" so hasRole("HR_ADMIN")
 * works without the "ROLE_" prefix in @PreAuthorize / SecurityConfig.
 */
public class EmployeePrincipal implements UserDetails {

    private final Employee employee;

    public EmployeePrincipal(Employee employee) {
        this.employee = employee;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getEmployeeId() {
        return employee.getId();
    }

    
    /** The company this staff member belongs to. Every HR query is limited to it. */
    public String getCompanyId() {
        String companyId = employee.getCompanyId();
        if (companyId == null) {
            // Never run a query with no company: it could match other companies' old records
            throw new IllegalStateException("Your account isn't linked to a company yet. Please contact HR Bots support.");
        }
        return companyId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
    }

    @Override
    public String getPassword() {
        return employee.getPassword();
    }

    @Override
    public String getUsername() {
        return employee.getEmail();
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
