package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.LeaveRequest;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class LeaveRequestDtos {

    // employeeId is intentionally NOT a field here - it's derived from the
    // authenticated principal in the controller, not trusted from the
    // client. Same for DecisionRequest.reviewerEmployeeId below.
    public record CreateLeaveRequest(
            @NotNull LeaveRequest.LeaveType leaveType,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String reason
    ) {}

    public record DecisionRequest(
            @NotNull LeaveRequest.Status decision, // APPROVED or REJECTED
            String note
    ) {}
}
