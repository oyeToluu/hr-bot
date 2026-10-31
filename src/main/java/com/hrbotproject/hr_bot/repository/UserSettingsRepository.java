package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.UserSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {
    Optional<UserSettings> findByEmployeeId(String employeeId);
}