package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.CreateLeaveRequest;
import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.DecisionRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequest create(String employeeId, CreateLeaveRequest req) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));

        int days = (int) ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;
        if (days <= 0) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        if (req.leaveType() == LeaveRequest.LeaveType.ANNUAL && days > employee.getLeaveBalance()) {
            throw new IllegalArgumentException(
                    "Insufficient leave balance. You have " + employee.getLeaveBalance() + " day(s) left.");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employeeId(employeeId)
                .leaveType(req.leaveType())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .daysRequested(days)
                .reason(req.reason())
                .build();

        return leaveRequestRepository.save(leaveRequest);
    }

    public List<LeaveRequest> findMine(String employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    public List<LeaveRequest> findAll(LeaveRequest.Status statusFilter) {
        return statusFilter == null
                ? leaveRequestRepository.findAllByOrderByCreatedAtDesc()
                : leaveRequestRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
    }

    public LeaveRequest decide(String id, DecisionRequest decisionReq, String reviewerEmployeeId) {
        if (decisionReq.decision() != LeaveRequest.Status.APPROVED
                && decisionReq.decision() != LeaveRequest.Status.REJECTED) {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }

        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Leave request not found: " + id));

        if (leaveRequest.getStatus() != LeaveRequest.Status.PENDING) {
            throw new IllegalStateException("Request already " + leaveRequest.getStatus());
        }

        leaveRequest.setStatus(decisionReq.decision());
        leaveRequest.setReviewedByEmployeeId(reviewerEmployeeId);
        leaveRequest.setReviewNote(decisionReq.note());
        leaveRequestRepository.save(leaveRequest);

        if (decisionReq.decision() == LeaveRequest.Status.APPROVED
                && leaveRequest.getLeaveType() == LeaveRequest.LeaveType.ANNUAL) {
            Employee employee = employeeRepository.findById(leaveRequest.getEmployeeId())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found"));
            employee.setLeaveBalance(employee.getLeaveBalance() - leaveRequest.getDaysRequested());
            employeeRepository.save(employee);
        }

        return leaveRequest;
    }
}
