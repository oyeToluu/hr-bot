package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.OnboardingFaq;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OnboardingFaqRepository extends MongoRepository<OnboardingFaq, String> {
    List<OnboardingFaq> findByCompanyId(String companyId);
}
