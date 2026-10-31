package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.SettingsDtos.UpdateSettingsRequest;
import com.hrbotproject.hr_bot.model.UserSettings;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Each employee's own settings. Whose settings comes from the login token, never the request. */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/me")
    public UserSettings mine(@AuthenticationPrincipal EmployeePrincipal me) {
        return settingsService.mine(me.getEmployeeId());
    }

    @PutMapping("/me")
    public UserSettings update(
            @AuthenticationPrincipal EmployeePrincipal me,
            @Valid @RequestBody UpdateSettingsRequest req
    ) {
        return settingsService.update(me.getEmployeeId(), req);
    }
}