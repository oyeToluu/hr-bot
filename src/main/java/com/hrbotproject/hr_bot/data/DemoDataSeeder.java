package com.hrbotproject.hr_bot.data;

import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.JobPosting;
import com.hrbotproject.hr_bot.model.OnboardingFaq;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.JobPostingRepository;
import com.hrbotproject.hr_bot.repository.OnboardingFaqRepository;
import com.hrbotproject.hr_bot.repository.PolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final OnboardingFaqRepository faqRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final JobPostingRepository jobPostingRepository;
    private final PasswordEncoder passwordEncoder;

    // Read from application.properties (SEED_ADMIN_PASSWORD / SEED_EMPLOYEE_PASSWORD
    // env vars when hosted), so the live admin password isn't the public default.
    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.employee-password}")
    private String employeePassword;

    @Override
    public void run(String... args) {
        if (employeeRepository.count() == 0) {
            employeeRepository.save(Employee.builder()
                    .fullName("Toni HR Admin")
                    .email("admin@hrbots.test")
                    .password(passwordEncoder.encode(adminPassword))
                    .department("Human Resources")
                    .role(Employee.Role.HR_ADMIN)
                    .build());

            employeeRepository.save(Employee.builder()
                    .fullName("Ada Okafor")
                    .email("ada@hrbots.test")
                    .password(passwordEncoder.encode(employeePassword))
                    .department("Engineering")
                    .role(Employee.Role.EMPLOYEE)
                    .leaveBalance(20)
                    .build());

            System.out.println("[seed] created demo employees: admin@hrbots.test, ada@hrbots.test");
        }

        if (faqRepository.count() == 0) {
            faqRepository.saveAll(List.of(
                    OnboardingFaq.builder()
                            .question("When do I get paid?")
                            .answer("Salaries are paid on the last working day of every month via bank transfer.")
                            .keywords(List.of("pay", "paid", "payroll", "salary", "payment"))
                            .category(OnboardingFaq.Category.PAYROLL)
                            .build(),
                    OnboardingFaq.builder()
                            .question("What equipment will I be issued?")
                            .answer("New hires receive a laptop, monitor, and access badge within your first 3 working days.")
                            .keywords(List.of("laptop", "equipment", "device", "badge", "monitor"))
                            .category(OnboardingFaq.Category.EQUIPMENT)
                            .build(),
                    OnboardingFaq.builder()
                            .question("What health benefits do I have?")
                            .answer("You are enrolled in the company HMO plan from your first day. HR shares your HMO card within 2 weeks.")
                            .keywords(List.of("benefits", "hmo", "health", "insurance", "medical"))
                            .category(OnboardingFaq.Category.BENEFITS)
                            .build(),
                    OnboardingFaq.builder()
                            .question("What are the office hours?")
                            .answer("Standard office hours are 9:00 AM to 5:00 PM, Monday to Friday, with a hybrid option after your first month.")
                            .keywords(List.of("hours", "office", "schedule", "time", "work"))
                            .category(OnboardingFaq.Category.WORKPLACE)
                            .build()
            ));
            System.out.println("[seed] created 4 onboarding FAQs");
        }

        if (policyDocumentRepository.count() == 0) {
            policyDocumentRepository.save(PolicyDocument.builder()
                    .title("Employee Handbook 2026")
                    .sections(List.of(
                            PolicyDocument.Section.builder()
                                    .heading("Leave Policy")
                                    .content("Employees accrue 20 annual leave days per year, plus 12 sick days. "
                                            + "Leave requests must be submitted at least 3 working days in advance except for sick leave.")
                                    .keywords(List.of("leave", "annual", "sick", "vacation", "pto", "days"))
                                    .build(),
                            PolicyDocument.Section.builder()
                                    .heading("Remote Work Policy")
                                    .content("Employees may work remotely up to 2 days per week after their probation "
                                            + "period (first 3 months), subject to manager approval.")
                                    .keywords(List.of("remote", "wfh", "hybrid", "work from home"))
                                    .build(),
                            PolicyDocument.Section.builder()
                                    .heading("Code of Conduct")
                                    .content("All employees are expected to treat colleagues with respect. Harassment, "
                                            + "discrimination, or abusive conduct of any kind will result in disciplinary "
                                            + "action up to and including termination.")
                                    .keywords(List.of("conduct", "harassment", "discrimination", "respect", "behavior"))
                                    .build()
                    ))
                    .build());
            System.out.println("[seed] created Employee Handbook 2026 policy document");
        }

        if (jobPostingRepository.count() == 0) {
            jobPostingRepository.saveAll(List.of(
                    JobPosting.builder()
                            .title("Junior Backend Developer")
                            .department("Engineering")
                            .location("Lagos (hybrid)")
                            .description("Build and maintain REST APIs for our HR platform using Java and Spring Boot.")
                            .requiredSkills(List.of("Java", "Spring Boot", "REST API", "MongoDB", "Git"))
                            .niceToHaveSkills(List.of("Docker", "JUnit", "React"))
                            .minYearsExperience(2)
                            .educationKeywords(List.of("BSc", "HND", "Computer Science"))
                            .autoRejectBelow(40)
                            .shortlistAt(70)
                            .build(),
                    JobPosting.builder()
                            .title("HR Assistant")
                            .department("Human Resources")
                            .location("Lagos")
                            .description("Support recruitment, onboarding and employee records.")
                            .requiredSkills(List.of("Recruitment", "Onboarding", "Microsoft Excel", "Communication"))
                            .niceToHaveSkills(List.of("CIPM", "HRIS", "Payroll"))
                            .minYearsExperience(1)
                            .educationKeywords(List.of("BSc", "HND"))
                            .autoRejectBelow(40)
                            .shortlistAt(70)
                            .build()
            ));
            System.out.println("[seed] created 2 demo job postings");
        }
    }
}
