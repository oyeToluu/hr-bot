package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.SettingsDtos.UpdateSettingsRequest;
import com.hrbotproject.hr_bot.model.UserSettings;
import com.hrbotproject.hr_bot.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final UserSettingsRepository repository;

    public SettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    /** The employee's saved settings, or the defaults if they've never changed anything. */
    public UserSettings mine(String employeeId) {
        return repository.findByEmployeeId(employeeId)
                .orElseGet(() -> UserSettings.defaultsFor(employeeId));
    }

    /** Changes only the fields that were sent, then saves. */
    public UserSettings update(String employeeId, UpdateSettingsRequest req) {
        UserSettings s = mine(employeeId);
        if (req.islamicFeatures() != null) s.setIslamicFeatures(req.islamicFeatures());
        if (req.city() != null) s.setCity(req.city());
        if (req.asrMethod() != null) s.setAsrMethod(req.asrMethod());
        if (req.taskReminders() != null) s.setTaskReminders(req.taskReminders());
        if (req.prayerReminders() != null) s.setPrayerReminders(req.prayerReminders());
        if (req.ramadanReminders() != null) s.setRamadanReminders(req.ramadanReminders());
        if (req.leaveAlerts() != null) s.setLeaveAlerts(req.leaveAlerts());
        if (req.tone() != null) s.setTone(req.tone());
        if (req.volume() != null) s.setVolume(req.volume());
        return repository.save(s);
    }
}