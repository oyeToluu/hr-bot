package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.JobRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.JobSummary;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.MyApplicationView;
import com.hrbotproject.hr_bot.model.JobApplication;
import com.hrbotproject.hr_bot.model.JobPosting;
import com.hrbotproject.hr_bot.security.CandidatePrincipal;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.JobApplicationService;
import com.hrbotproject.hr_bot.service.JobPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// Who can call what is set in SecurityConfig:
//   GET  /api/jobs, /api/jobs/{id}          anyone (public job board)
//   POST /api/jobs/{id}/apply               CANDIDATE
//   everything else under /api/jobs         HR_ADMIN
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobPostingService jobPostingService;
    private final JobApplicationService jobApplicationService;

    @GetMapping
    public List<JobPosting> openJobs() {
        return jobPostingService.listOpen();
    }

    @GetMapping("/{id}")
    public JobPosting openJob(@PathVariable String id) {
        return jobPostingService.getOpen(id);
    }

    // Multipart form: "cv" = the file (PDF, DOCX, TXT or a ZIP of one), "coverNote" optional
    @PostMapping(value = "/{id}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MyApplicationView> apply(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable String id,
            @RequestParam("cv") MultipartFile cv,
            @RequestParam(value = "coverNote", required = false) String coverNote
    ) throws IOException {
        MyApplicationView result = jobApplicationService.apply(id, principal.getCandidateId(),
                cv.getOriginalFilename(), cv.getContentType(), cv.getBytes(), coverNote);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ---- HR admin ----

    @GetMapping("/manage")
    public List<JobSummary> allJobs() {
        return jobPostingService.listForHr();
    }

    @PostMapping
    public ResponseEntity<JobPosting> create(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @Valid @RequestBody JobRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobPostingService.create(req, principal.getEmployeeId()));
    }

    @PutMapping("/{id}")
    public JobPosting update(@PathVariable String id, @Valid @RequestBody JobRequest req) {
        return jobPostingService.update(id, req);
    }

    @PatchMapping("/{id}/close")
    public JobPosting close(@PathVariable String id) {
        return jobPostingService.setOpen(id, false);
    }

    @PatchMapping("/{id}/reopen")
    public JobPosting reopen(@PathVariable String id) {
        return jobPostingService.setOpen(id, true);
    }

    // Ranked by the screening bot, best fit first
    @GetMapping("/{id}/applications")
    public List<JobApplication> applications(@PathVariable String id) {
        return jobApplicationService.forJob(id);
    }
}
