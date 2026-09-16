package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.CreateLeaveRequest;
import com.hrbotproject.hr_bot.dto.LeaveRequestDtos.DecisionRequest;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    // Submit a leave request - employeeId comes from the JWT, never the body
    @PostMapping
    public ResponseEntity<LeaveRequest> create(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @Valid @RequestBody CreateLeaveRequest req
    ) {
        LeaveRequest created = leaveRequestService.create(principal.getEmployeeId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // The logged-in employee's own request history
    @GetMapping("/mine")
    public List<LeaveRequest> mine(@AuthenticationPrincipal EmployeePrincipal principal) {
        return leaveRequestService.findMine(principal.getEmployeeId());
    }

    // HR only (enforced in SecurityConfig): all requests, optional ?status=PENDING filter
    @GetMapping
    public List<LeaveRequest> all(@RequestParam(required = false) LeaveRequest.Status status) {
        return leaveRequestService.findAll(status);
    }

    // HR only (enforced in SecurityConfig): approve or reject
    @PatchMapping("/{id}/decision")
    public LeaveRequest decide(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody DecisionRequest req
    ) {
        return leaveRequestService.decide(id, req, principal.getEmployeeId());
    }
}
