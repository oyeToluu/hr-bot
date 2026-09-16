package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;

import java.util.List;

public class DashboardDtos {

    public record EmployeeDashboard(
            Employee profile,
            List<LeaveRequest> recentLeaveRequests,
            long pendingCount
    ) {}

    public record AdminDashboard(
            long totalEmployees,
            long onboardingIncomplete,
            long pendingLeaveRequests,
            long approvedLeaveRequests,
            long rejectedLeaveRequests
    ) {}
}
