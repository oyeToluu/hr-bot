package com.hrbotproject.hr_bot.controller;

import tools.jackson.databind.ObjectMapper;
import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.CreateLeaveRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.security.JwtAuthenticationFilter;
import com.hrbotproject.hr_bot.security.SecurityConfig;
import com.hrbotproject.hr_bot.service.LeaveRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Spring Boot 4 notes:
//  - @MockBean was removed; @MockitoBean replaces it.
//  - @WebMvcTest does NOT load our SecurityConfig by itself. Without it,
//    Spring's default security chain applies, which has CSRF on and turns
//    every POST into 403. @Import below loads the REAL SecurityConfig
//    (CSRF off, JWT filter, 401 for missing token), so these tests check
//    the same rules the web and Android apps will hit.
@WebMvcTest(LeaveRequestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @MockitoBean
    private com.hrbotproject.hr_bot.security.JwtService jwtService;

    @MockitoBean
    private com.hrbotproject.hr_bot.security.EmployeeUserDetailsService userDetailsService;

    private final Employee employee = Employee.builder()
            .id("emp-1")
            .fullName("Ada Okafor")
            .email("ada@hrbots.test")
            .role(Employee.Role.EMPLOYEE)
            .build();

    private final EmployeePrincipal principal = new EmployeePrincipal(employee);

    @Test
    void createLeaveRequest_returns201WithBody() throws Exception {
        LeaveRequest saved = LeaveRequest.builder()
                .id("leave-1")
                .employeeId("emp-1")
                .leaveType(LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 5))
                .daysRequested(5)
                .status(LeaveRequest.Status.PENDING)
                .build();

        when(leaveRequestService.create(eq("emp-1"), any())).thenReturn(saved);

        CreateLeaveRequest body = new CreateLeaveRequest(
                LeaveRequest.LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "Trip");

        mockMvc.perform(post("/api/leave")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("leave-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.daysRequested").value(5));
    }

    @Test
    void createLeaveRequest_missingFields_returns400() throws Exception {
        // leaveType, startDate, endDate are all @NotNull - send an empty body
        mockMvc.perform(post("/api/leave")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLeaveRequest_unauthenticated_returns401() throws Exception {
        CreateLeaveRequest body = new CreateLeaveRequest(
                LeaveRequest.LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "Trip");

        mockMvc.perform(post("/api/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myLeaveRequests_returnsCallersOwnRequests() throws Exception {
        when(leaveRequestService.findMine("emp-1")).thenReturn(List.of(
                LeaveRequest.builder().id("leave-1").employeeId("emp-1").build(),
                LeaveRequest.builder().id("leave-2").employeeId("emp-1").build()
        ));

        mockMvc.perform(get("/api/leave/mine").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
