package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    boolean existsByJobIdAndCandidateId(String jobId, String candidateId);
    List<JobApplication> findByCandidateIdOrderByAppliedAtDesc(String candidateId);
    List<JobApplication> findByJobIdOrderByFinalScoreDesc(String jobId);
    long countByJobId(String jobId);
    long countByStatus(JobApplication.Status status);
}
