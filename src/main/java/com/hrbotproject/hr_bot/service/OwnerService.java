package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.OwnerDtos.CompanySummary;
import com.hrbotproject.hr_bot.dto.OwnerDtos.HrContact;
import com.hrbotproject.hr_bot.dto.OwnerDtos.OwnerOverview;
import com.hrbotproject.hr_bot.model.Company;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.CompanyRepository;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.JobPostingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The App Owner (whoever owns HR Bots) sees every company that signed up,
 * as a summary only: no employee's private HR records.
 * The owner is set by email in APP_OWNER_EMAILS, never by anything a user can change.
 */
@Service
public class OwnerService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final JobPostingRepository jobRepository;
    private final Set<String> ownerEmails;

    public OwnerService(
            CompanyRepository companyRepository,
            EmployeeRepository employeeRepository,
            JobPostingRepository jobRepository,
            @Value("${app.owner-emails:erotimi867@gmail.com}") String ownerEmails
    ) {
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.jobRepository = jobRepository;
        this.ownerEmails = Arrays.stream(ownerEmails.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean isOwner(Employee e) {
        return e != null && e.getEmail() != null
                && !Boolean.FALSE.equals(e.getEmailVerified())
                && ownerEmails.contains(e.getEmail().trim().toLowerCase(Locale.ROOT));
    }

    public OwnerOverview overview(Employee me) {
        if (!isOwner(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the HR Bots owner can see this page");
        }
        List<CompanySummary> list = companyRepository.findAll().stream()
                .map(this::summary)
                .sorted(Comparator.comparing(CompanySummary::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        long onTrial = list.stream().filter(c -> c.plan() == Company.Plan.FREE_TRIAL).count();
        long totalStaff = list.stream().mapToLong(CompanySummary::staffCount).sum();
        return new OwnerOverview(list.size(), onTrial, list.size() - onTrial, totalStaff, list);
    }

    private CompanySummary summary(Company c) {
        List<HrContact> hr = employeeRepository.findByCompanyIdAndRole(c.getId(), Employee.Role.HR_ADMIN).stream()
                .map(e -> new HrContact(e.getFullName(), e.getEmail()))
                .toList();
        return new CompanySummary(c.getId(), c.getName(), c.getSlug(), c.getIndustry(), c.getPlan(),
                c.getTrialEndsAt(), c.getCreatedAt(), employeeRepository.countByCompanyId(c.getId()),
                jobRepository.countByCompanyIdAndOpenTrue(c.getId()), hr);
    }
}