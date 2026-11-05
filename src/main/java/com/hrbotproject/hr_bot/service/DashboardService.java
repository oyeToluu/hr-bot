package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.DashboardDtos.AdminDashboard;
import com.hrbotproject.hr_bot.dto.DashboardDtos.EmployeeDashboard;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public EmployeeDashboard myDashboard(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));

        List<LeaveRequest> recent = leaveRequestRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .limit(10)
                .toList();

        long pending = recent.stream().filter(r -> r.getStatus() == LeaveRequest.Status.PENDING).count();

        return new EmployeeDashboard(employee, recent, pending);
    }

    public AdminDashboard adminDashboard(String companyId) {
        long totalEmployees = employeeRepository.countByCompanyIdAndRole(companyId, Employee.Role.EMPLOYEE);
        long onboardingIncomplete = employeeRepository
                .countByCompanyIdAndRoleAndOnboardingCompleteFalse(companyId, Employee.Role.EMPLOYEE);

        long pending = leaveRequestRepository.countByCompanyIdAndStatus(companyId, LeaveRequest.Status.PENDING);
        long approved = leaveRequestRepository.countByCompanyIdAndStatus(companyId, LeaveRequest.Status.APPROVED);
        long rejected = leaveRequestRepository.countByCompanyIdAndStatus(companyId, LeaveRequest.Status.REJECTED);

        return new AdminDashboard(totalEmployees, onboardingIncomplete, pending, approved, rejected);
    }
}
