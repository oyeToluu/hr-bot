package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.CreateLeaveRequest;
import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.DecisionRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private LeaveRequestService leaveRequestService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        leaveRequestService = new LeaveRequestService(leaveRequestRepository, employeeRepository);
        employee = Employee.builder()
                .id("emp-1")
                .fullName("Ada Okafor")
                .email("ada@hrbots.test")
                .role(Employee.Role.EMPLOYEE)
                .leaveBalance(20)
                .build();
    }

    @Test
    @DisplayName("create() saves a valid annual leave request within balance")
    void create_savesValidRequest() {
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateLeaveRequest req = new CreateLeaveRequest(
                LeaveRequest.LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "Family trip");

        LeaveRequest result = leaveRequestService.create("emp-1", req);

        assertThat(result.getDaysRequested()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo(LeaveRequest.Status.PENDING);
        assertThat(result.getEmployeeId()).isEqualTo("emp-1");
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("create() rejects annual leave that exceeds remaining balance")
    void create_rejectsWhenBalanceInsufficient() {
        employee.setLeaveBalance(3);
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

        CreateLeaveRequest req = new CreateLeaveRequest(
                LeaveRequest.LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), "Too long");

        assertThatThrownBy(() -> leaveRequestService.create("emp-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient leave balance");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() rejects an end date before the start date")
    void create_rejectsInvalidDateRange() {
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

        CreateLeaveRequest req = new CreateLeaveRequest(
                LeaveRequest.LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), null);

        assertThatThrownBy(() -> leaveRequestService.create("emp-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must be on or after startDate");
    }

    @Test
    @DisplayName("create() throws when the employee does not exist")
    void create_throwsWhenEmployeeMissing() {
        when(employeeRepository.findById("ghost")).thenReturn(Optional.empty());

        CreateLeaveRequest req = new CreateLeaveRequest(
                LeaveRequest.LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), null);

        assertThatThrownBy(() -> leaveRequestService.create("ghost", req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("decide() approving annual leave deducts the balance")
    void decide_approvedAnnualLeaveDeductsBalance() {
        LeaveRequest pending = LeaveRequest.builder()
                .id("leave-1")
                .employeeId("emp-1")
                .leaveType(LeaveRequest.LeaveType.ANNUAL)
                .daysRequested(4)
                .status(LeaveRequest.Status.PENDING)
                .build();

        when(leaveRequestRepository.findById("leave-1")).thenReturn(Optional.of(pending));
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

        DecisionRequest decision = new DecisionRequest(LeaveRequest.Status.APPROVED, "Approved, enjoy!");
        LeaveRequest result = leaveRequestService.decide("leave-1", decision, "hr-1");

        assertThat(result.getStatus()).isEqualTo(LeaveRequest.Status.APPROVED);
        assertThat(result.getReviewedByEmployeeId()).isEqualTo("hr-1");

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getLeaveBalance()).isEqualTo(16); // 20 - 4
    }

    @Test
    @DisplayName("decide() rejecting leave does not touch the employee's balance")
    void decide_rejectedLeaveDoesNotDeductBalance() {
        LeaveRequest pending = LeaveRequest.builder()
                .id("leave-2")
                .employeeId("emp-1")
                .leaveType(LeaveRequest.LeaveType.ANNUAL)
                .daysRequested(3)
                .status(LeaveRequest.Status.PENDING)
                .build();

        when(leaveRequestRepository.findById("leave-2")).thenReturn(Optional.of(pending));

        DecisionRequest decision = new DecisionRequest(LeaveRequest.Status.REJECTED, "Team is short-staffed");
        LeaveRequest result = leaveRequestService.decide("leave-2", decision, "hr-1");

        assertThat(result.getStatus()).isEqualTo(LeaveRequest.Status.REJECTED);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("decide() throws on an already-decided request")
    void decide_throwsWhenAlreadyDecided() {
        LeaveRequest alreadyApproved = LeaveRequest.builder()
                .id("leave-3")
                .employeeId("emp-1")
                .leaveType(LeaveRequest.LeaveType.SICK)
                .daysRequested(2)
                .status(LeaveRequest.Status.APPROVED)
                .build();

        when(leaveRequestRepository.findById("leave-3")).thenReturn(Optional.of(alreadyApproved));

        DecisionRequest decision = new DecisionRequest(LeaveRequest.Status.REJECTED, null);

        assertThatThrownBy(() -> leaveRequestService.decide("leave-3", decision, "hr-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }

    @Test
    @DisplayName("findMine() delegates to the repository for the given employee")
    void findMine_delegatesToRepository() {
        List<LeaveRequest> expected = List.of(LeaveRequest.builder().id("leave-1").employeeId("emp-1").build());
        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc("emp-1")).thenReturn(expected);

        List<LeaveRequest> result = leaveRequestService.findMine("emp-1");

        assertThat(result).isEqualTo(expected);
    }
}
