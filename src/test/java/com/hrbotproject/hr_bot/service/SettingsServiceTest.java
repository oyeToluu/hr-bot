package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.SettingsDtos.UpdateSettingsRequest;
import com.hrbotproject.hr_bot.model.UserSettings;
import com.hrbotproject.hr_bot.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserSettingsRepository repository;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(repository);
    }

    @Test
    @DisplayName("mine() gives sensible defaults without saving anything")
    void mine_defaults() {
        when(repository.findByEmployeeId("ada")).thenReturn(Optional.empty());

        UserSettings s = settingsService.mine("ada");

        assertThat(s.getEmployeeId()).isEqualTo("ada");
        assertThat(s.isIslamicFeatures()).isFalse();
        assertThat(s.getCity()).isEqualTo("LAGOS");
        assertThat(s.isTaskReminders()).isTrue();
        assertThat(s.getVolume()).isEqualTo(70);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update() changes only the fields that were sent")
    void update_onlyChangesSentFields() {
        UserSettings saved = UserSettings.defaultsFor("ada");
        saved.setTone("gold-bell");
        when(repository.findByEmployeeId("ada")).thenReturn(Optional.of(saved));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSettings s = settingsService.update("ada",
                new UpdateSettingsRequest(true, "ABUJA", null, null, null, null, null, null, 40));

        assertThat(s.isIslamicFeatures()).isTrue();
        assertThat(s.getCity()).isEqualTo("ABUJA");
        assertThat(s.getVolume()).isEqualTo(40);
        assertThat(s.getTone()).isEqualTo("gold-bell");   // not sent, so unchanged
        assertThat(s.getAsrMethod()).isEqualTo("STANDARD");
    }
}