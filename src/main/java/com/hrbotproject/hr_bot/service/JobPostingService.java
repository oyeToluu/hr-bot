package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.JobRequest;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.JobSummary;
import com.hrbotproject.hr_bot.model.JobPosting;
import com.hrbotproject.hr_bot.repository.JobApplicationRepository;
import com.hrbotproject.hr_bot.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository jobRepository;
    private final JobApplicationRepository applicationRepository;

    /** Public job board: open jobs only. */
    public List<JobPosting> listOpen() {
        return jobRepository.findByOpenTrueOrderByCreatedAtDesc();
    }

    /** Public job detail. Closed jobs are hidden from the public. */
    public JobPosting getOpen(String id) {
        JobPosting job = findById(id);
        if (!job.isOpen()) {
            throw new NoSuchElementException("This job is no longer accepting applications");
        }
        return job;
    }

    /** HR view: every job with its application count. */
    public List<JobSummary> listForHr() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(j -> new JobSummary(j.getId(), j.getTitle(), j.getDepartment(), j.getLocation(),
                        j.isOpen(), applicationRepository.countByJobId(j.getId()), j.getCreatedAt()))
                .toList();
    }

    public JobPosting create(JobRequest req, String hrEmployeeId) {
        JobPosting job = new JobPosting();
        job.setOpen(true);
        job.setCreatedBy(hrEmployeeId);
        apply(job, req);
        return jobRepository.save(job);
    }

    public JobPosting update(String id, JobRequest req) {
        JobPosting job = findById(id);
        apply(job, req);
        return jobRepository.save(job);
    }

    public JobPosting setOpen(String id, boolean open) {
        JobPosting job = findById(id);
        job.setOpen(open);
        return jobRepository.save(job);
    }

    public JobPosting findById(String id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Job not found: " + id));
    }

    private static void apply(JobPosting job, JobRequest req) {
        int reject = req.autoRejectBelow() == null ? 40 : req.autoRejectBelow();
        int shortlist = req.shortlistAt() == null ? 70 : req.shortlistAt();
        if (reject > shortlist) {
            throw new IllegalArgumentException("The auto-reject score must not be higher than the shortlist score");
        }
        job.setTitle(req.title().trim());
        job.setDepartment(req.department());
        job.setLocation(req.location());
        job.setDescription(req.description());
        job.setRequiredSkills(clean(req.requiredSkills()));
        job.setNiceToHaveSkills(clean(req.niceToHaveSkills()));
        job.setEducationKeywords(clean(req.educationKeywords()));
        job.setMinYearsExperience(req.minYearsExperience() == null ? 0 : req.minYearsExperience());
        job.setAutoRejectBelow(reject);
        job.setShortlistAt(shortlist);
    }

    // Trim, drop blanks and duplicates (case-insensitive), keep HR's order.
    private static List<String> clean(List<String> items) {
        List<String> out = new ArrayList<>();
        if (items == null) return out;
        for (String item : items) {
            if (item == null) continue;
            String s = item.trim();
            if (!s.isEmpty() && out.stream().noneMatch(o -> o.equalsIgnoreCase(s))) {
                out.add(s);
            }
        }
        return out;
    }
}
