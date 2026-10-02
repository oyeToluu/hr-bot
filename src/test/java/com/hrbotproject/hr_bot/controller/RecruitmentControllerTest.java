package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.MyApplicationView;
import com.hrbotproject.hr_bot.model.Candidate;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.JobApplication;
import com.hrbotproject.hr_bot.model.JobPosting;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.security.EmployeeUserDetailsService;
import com.hrbotproject.hr_bot.security.JwtAuthenticationFilter;
import com.hrbotproject.hr_bot.security.JwtService;
import com.hrbotproject.hr_bot.security.SecurityConfig;
import com.hrbotproject.hr_bot.service.JobApplicationService;
import com.hrbotproject.hr_bot.service.JobPostingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks the recruitment access rules in the real SecurityConfig:
// public job board, candidates can apply, staff and candidates can't
// reach each other's endpoints.
@WebMvcTest({JobController.class, JobApplicationController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RecruitmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JobPostingService jobPostingService;
    @MockitoBean private JobApplicationService jobApplicationService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private EmployeeUserDetailsService userDetailsService;

    private final CandidatePrincipal candidate = new CandidatePrincipal(
            Candidate.builder().id("cand-1").fullName("Chidi Eze").email("chidi@example.com").build());

    private final EmployeePrincipal employee = new EmployeePrincipal(
            Employee.builder().id("emp-1").fullName("Ada").email("ada@hrbots.test").role(Employee.Role.EMPLOYEE).build());

    private final MockMultipartFile cvFile =
            new MockMultipartFile("cv", "cv.pdf", "application/pdf", new byte[]{1, 2, 3});

    @Test
    void jobBoard_isPublic() throws Exception {
        when(jobPostingService.listOpen()).thenReturn(List.of(JobPosting.builder().id("job-1").title("Dev").build()));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Dev"));
    }

    @Test
    void candidate_canApplyWithCv() throws Exception {
        when(jobApplicationService.apply(eq("job-1"), eq("cand-1"), eq("cv.pdf"), any(), any(), any()))
                .thenReturn(new MyApplicationView("app-1", "job-1", "Dev", JobApplication.Status.SHORTLISTED,
                        82, List.of("Java"), List.of(), false, LocalDateTime.now()));

        mockMvc.perform(multipart("/api/jobs/job-1/apply").file(cvFile).with(user(candidate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"))
                .andExpect(jsonPath("$.score").value(82));
    }

    @Test
    void applying_requiresLogin() throws Exception {
        mockMvc.perform(multipart("/api/jobs/job-1/apply").file(cvFile))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employee_cannotApply() throws Exception {
        mockMvc.perform(multipart("/api/jobs/job-1/apply").file(cvFile).with(user(employee)))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidate_cannotSeeHrApplicantList() throws Exception {
        mockMvc.perform(get("/api/jobs/job-1/applications").with(user(candidate)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/jobs/manage").with(user(candidate)))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidate_cannotReachEmployeeEndpoints() throws Exception {
        // Leave, dashboard, onboarding etc. are staff-only
        mockMvc.perform(get("/api/leave/mine").with(user(candidate)))
                .andExpect(status().isForbidden());
    }
}
