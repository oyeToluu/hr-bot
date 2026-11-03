package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.EmailCode;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailCodeRepository extends MongoRepository<EmailCode, String> {
}