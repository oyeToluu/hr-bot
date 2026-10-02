package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.CvDownload;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.DecisionRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.MyApplicationView;
import com.hrbotproject.hr_bot.model.JobApplication;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

// /api/applications/mine is CANDIDATE only; the rest is HR_ADMIN (SecurityConfig).
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @GetMapping("/mine")
    public List<MyApplicationView> mine(@AuthenticationPrincipal CandidatePrincipal principal) {
        return jobApplicationService.mine(principal.getCandidateId());
    }

    @GetMapping("/{id}")
    public JobApplication get(@PathVariable String id) {
        return jobApplicationService.findById(id);
    }

    @GetMapping("/{id}/cv")
    public ResponseEntity<byte[]> downloadCv(@PathVariable String id) {
        CvDownload cv = jobApplicationService.downloadCv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(cv.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(cv.contentType()))
                .body(cv.bytes());
    }

    // The human takes over: INTERVIEW, HIRED, REJECTED, or REINSTATE an auto-reject
    @PatchMapping("/{id}/decision")
    public JobApplication decide(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody DecisionRequest req
    ) {
        return jobApplicationService.decide(id, req, principal.getEmployeeId());
    }
}
