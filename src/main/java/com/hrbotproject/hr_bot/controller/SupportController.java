package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.SupportDtos.OpenTicketRequest;
import com.hrbotproject.hr_bot.dto.SupportDtos.ReplyRequest;
import com.hrbotproject.hr_bot.dto.SupportDtos.SupportCounts;
import com.hrbotproject.hr_bot.model.SupportTicket;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/support/inbox/** is HR_ADMIN only (SecurityConfig); the rest is any staff member's own requests.
@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    // ---- employee: my own requests ----

    @PostMapping
    public ResponseEntity<SupportTicket> open(@AuthenticationPrincipal EmployeePrincipal me,
                                              @Valid @RequestBody OpenTicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.open(me.getEmployee(), req));
    }

    @GetMapping("/mine")
    public List<SupportTicket> mine(@AuthenticationPrincipal EmployeePrincipal me) {
        return supportService.mine(me.getEmployeeId());
    }

    @GetMapping("/mine/counts")
    public SupportCounts myCounts(@AuthenticationPrincipal EmployeePrincipal me) {
        return supportService.countsForEmployee(me.getEmployeeId());
    }

    @GetMapping("/mine/{id}")
    public SupportTicket mineOne(@AuthenticationPrincipal EmployeePrincipal me, @PathVariable String id) {
        return supportService.mineOne(me.getEmployee(), id);
    }

    @PostMapping("/mine/{id}/reply")
    public SupportTicket reply(@AuthenticationPrincipal EmployeePrincipal me, @PathVariable String id,
                               @Valid @RequestBody ReplyRequest req) {
        return supportService.employeeReply(me.getEmployee(), id, req.message());
    }

    @PatchMapping("/mine/{id}/close")
    public SupportTicket close(@AuthenticationPrincipal EmployeePrincipal me, @PathVariable String id) {
        return supportService.employeeClose(me.getEmployee(), id);
    }

    // ---- HR admin: requests from my company's staff ----

    @GetMapping("/inbox")
    public List<SupportTicket> inbox(@AuthenticationPrincipal EmployeePrincipal hr,
                                     @RequestParam(required = false) SupportTicket.Status status) {
        return supportService.inbox(hr.getCompanyId(), status);
    }

    @GetMapping("/inbox/counts")
    public SupportCounts inboxCounts(@AuthenticationPrincipal EmployeePrincipal hr) {
        return supportService.countsForHr(hr.getCompanyId());
    }

    @GetMapping("/inbox/{id}")
    public SupportTicket inboxOne(@AuthenticationPrincipal EmployeePrincipal hr, @PathVariable String id) {
        return supportService.inboxOne(hr.getCompanyId(), id);
    }

    @PostMapping("/inbox/{id}/reply")
    public SupportTicket hrReply(@AuthenticationPrincipal EmployeePrincipal hr, @PathVariable String id,
                                 @Valid @RequestBody ReplyRequest req) {
        return supportService.hrReply(hr.getEmployee(), id, req.message());
    }

    @PatchMapping("/inbox/{id}/close")
    public SupportTicket hrClose(@AuthenticationPrincipal EmployeePrincipal hr, @PathVariable String id) {
        return supportService.hrClose(hr.getCompanyId(), id);
    }
}