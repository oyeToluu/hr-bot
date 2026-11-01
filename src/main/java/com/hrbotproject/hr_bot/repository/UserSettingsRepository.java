package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.UserSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {
    List<UserSettings> findAllByEmployeeId(String employeeId);
}