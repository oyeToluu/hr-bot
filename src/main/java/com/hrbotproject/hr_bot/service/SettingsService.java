package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.SettingsDtos.UpdateSettingsRequest;
import com.hrbotproject.hr_bot.model.UserSettings;
import com.hrbotproject.hr_bot.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SettingsService {

    private final UserSettingsRepository repository;

    public SettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    /** The employee's saved settings, or the defaults if they've never changed anything. */
    public UserSettings mine(String employeeId) {
                return pick(employeeId, repository.findAllByEmployeeId(employeeId));
    }

    /** Changes only the fields that were sent, then saves. */
    public UserSettings update(String employeeId, UpdateSettingsRequest req) {
        List<UserSettings> existing = repository.findAllByEmployeeId(employeeId);
        List<String> oldCopies = existing.stream()
                .map(UserSettings::getId)
                .filter(id -> !employeeId.equals(id))
                .toList();
        UserSettings s = pick(employeeId, existing);
        if (req.islamicFeatures() != null) s.setIslamicFeatures(req.islamicFeatures());
        if (req.city() != null) s.setCity(req.city());
        if (req.asrMethod() != null) s.setAsrMethod(req.asrMethod());
        if (req.taskReminders() != null) s.setTaskReminders(req.taskReminders());
        if (req.prayerReminders() != null) s.setPrayerReminders(req.prayerReminders());
        if (req.ramadanReminders() != null) s.setRamadanReminders(req.ramadanReminders());
        if (req.leaveAlerts() != null) s.setLeaveAlerts(req.leaveAlerts());
        if (req.tone() != null) s.setTone(req.tone());
        if (req.volume() != null) s.setVolume(req.volume());
          s.setId(employeeId);
        UserSettings saved = repository.save(s);
         oldCopies.forEach(repository::deleteById);
        return saved;
    }
        private static UserSettings pick(String employeeId, List<UserSettings> found) {
        return found.stream()
                .filter(s -> employeeId.equals(s.getId()))
                .findFirst()
                .or(() -> found.stream().findFirst())
                .orElseGet(() -> UserSettings.defaultsFor(employeeId));
    }
    
}