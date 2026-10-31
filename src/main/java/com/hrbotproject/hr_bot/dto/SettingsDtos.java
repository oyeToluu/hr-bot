package com.hrbotproject.hr_bot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class SettingsDtos {

    /** Changing settings. Every field is optional; only the ones sent are changed. */
    public record UpdateSettingsRequest(
            Boolean islamicFeatures,
            @Pattern(regexp = "[A-Z_]{2,30}", message = "city is not recognised") String city,
            @Pattern(regexp = "STANDARD|HANAFI", message = "asrMethod must be STANDARD or HANAFI") String asrMethod,
            Boolean taskReminders,
            Boolean prayerReminders,
            Boolean ramadanReminders,
            Boolean leaveAlerts,
            @Pattern(regexp = "[a-z-]{2,30}", message = "tone is not recognised") String tone,
            @Min(value = 0, message = "volume must be 0 to 100") @Max(value = 100, message = "volume must be 0 to 100") Integer volume
    ) {}
}