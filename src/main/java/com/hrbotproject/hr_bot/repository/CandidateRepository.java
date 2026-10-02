package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.Candidate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CandidateRepository extends MongoRepository<Candidate, String> {
    Optional<Candidate> findByEmail(String email);
    boolean existsByEmail(String email);
}
