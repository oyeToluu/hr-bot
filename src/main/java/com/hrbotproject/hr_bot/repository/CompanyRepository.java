package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CompanyRepository extends MongoRepository<Company, String> {
    Optional<Company> findBySlug(String slug);
    Optional<Company> findByJoinCode(String joinCode);
    boolean existsBySlug(String slug);
}