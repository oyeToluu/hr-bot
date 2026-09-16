package com.hrbotproject.hr_bot.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "leave_requests")
public class LeaveRequest {

    @Id
    private String id;

    @NotNull
    private String employeeId;

    public enum LeaveType { ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID }

    @NotNull
    private LeaveType leaveType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private int daysRequested;

    private String reason;

    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED }

    @Builder.Default
    private Status status = Status.PENDING;

    private String reviewedByEmployeeId;

    private String reviewNote;

    @CreatedDate
    private LocalDateTime createdAt;
}
