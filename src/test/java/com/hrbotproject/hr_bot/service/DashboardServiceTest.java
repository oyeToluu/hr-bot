package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.DashboardDtos.AdminDashboard;
import com.hrbotproject.hr_bot.dto.DashboardDtos.EmployeeDashboard;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

     @Mock
     private EmployeeRepository employeeRepository;

     @Mock
     private LeaveRequestRepository leaveRequestRepository;

     private DashboardService dashboardService;
 
     @BeforeEach
     void setUp() {
        dashboardService = new DashboardService(employeeRepository, leaveRequestRepository);
    }

       @Test
       @DisplayName("myDashboard() returns profile plus up to 10 recent requests and a pending count")
        void myDashboard_returnsProfileAndRecentRequests() {
        Employee employee = Employee.builder().id("emp-1").fullName("Ada").email("ada@hrbots.test").build();
        List<LeaveRequest> requests = List.of(
                LeaveRequest.builder().id("l1").employeeId("emp-1").status(LeaveRequest.Status.PENDING).build(),
                LeaveRequest.builder().id("l2").employeeId("emp-1").status(LeaveRequest.Status.APPROVED).build()
        );

        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc("emp-1")).thenReturn(requests);

        EmployeeDashboard dashboard = dashboardService.myDashboard("emp-1");

        assertThat(dashboard.profile().getId()).isEqualTo("emp-1");
        assertThat(dashboard.recentLeaveRequests()).hasSize(2);
        assertThat(dashboard.pendingCount()).isEqualTo(1);
    }
        @Test
        @DisplayName("adminDashboard() counts headcount, onboarding gaps, and leave for one company")
        void adminDashboard_aggregatesCounts() {
        when(employeeRepository.countByCompanyIdAndRole("co-1", Employee.Role.EMPLOYEE)).thenReturn(10L);
        when(employeeRepository.countByCompanyIdAndRoleAndOnboardingCompleteFalse("co-1", Employee.Role.EMPLOYEE)).thenReturn(3L);
        when(leaveRequestRepository.countByCompanyIdAndStatus("co-1", LeaveRequest.Status.PENDING)).thenReturn(2L);
        when(leaveRequestRepository.countByCompanyIdAndStatus("co-1", LeaveRequest.Status.APPROVED)).thenReturn(1L);
        when(leaveRequestRepository.countByCompanyIdAndStatus("co-1", LeaveRequest.Status.REJECTED)).thenReturn(0L);

        AdminDashboard dashboard = dashboardService.adminDashboard("co-1");
        assertThat(dashboard.totalEmployees()).isEqualTo(10);
        assertThat(dashboard.onboardingIncomplete()).isEqualTo(3);
        assertThat(dashboard.pendingLeaveRequests()).isEqualTo(2);
        assertThat(dashboard.approvedLeaveRequests()).isEqualTo(1);
        assertThat(dashboard.rejectedLeaveRequests()).isZero();
    }
}
