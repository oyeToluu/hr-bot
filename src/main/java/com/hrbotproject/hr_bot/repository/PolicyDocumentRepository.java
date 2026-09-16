package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.PolicyDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PolicyDocumentRepository extends MongoRepository<PolicyDocument, String> {
}
