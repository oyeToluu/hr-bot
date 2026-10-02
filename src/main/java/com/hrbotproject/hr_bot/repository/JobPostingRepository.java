package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.JobPosting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface JobPostingRepository extends MongoRepository<JobPosting, String> {
    List<JobPosting> findByOpenTrueOrderByCreatedAtDesc();
    List<JobPosting> findAllByOrderByCreatedAtDesc();
}
