package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Company;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CompanyDtos {

    /** A business signing up: the company plus its first HR admin. */
    public record RegisterCompanyRequest(
            @NotBlank @Size(max = 100, message = "company name must be 100 characters or fewer") String companyName,
            @Size(max = 60, message = "industry must be 60 characters or fewer") String industry,
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = SecurityDtos.PASSWORD_RULE, message = SecurityDtos.PASSWORD_HINT) String password
    ) {}

    /**
     * What staff see about their own company. joinLink is only filled in for
     * HR admins, because anyone with the link can join as an employee.
     */
    public record CompanyView(
            String id,
            String name,
            String slug,
            String industry,
            Company.Plan plan,
            LocalDateTime trialEndsAt,
            long staffCount,
            String joinLink
    ) {}

    /** HR sending the join link to a new staff member. */
    public record InviteRequest(@NotBlank @Email String email) {}

    /** The little that anyone may see, e.g. on the "Join Zenith Foods Ltd" page. */
    public record PublicCompany(String name, String slug, String industry) {
        public static PublicCompany of(Company c) {
            return new PublicCompany(c.getName(), c.getSlug(), c.getIndustry());
        }
    }
}