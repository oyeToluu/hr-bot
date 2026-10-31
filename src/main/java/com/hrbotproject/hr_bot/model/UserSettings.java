package com.hrbotproject.hr_bot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One employee's personal preferences: calendar features, alerts and alert tune.
 * Stored on the server so they follow the employee to any device.
 */
@Document(collection = "user_settings")
public class UserSettings {

    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeId;

    /** Hijri dates, Islamic dates, prayer times, Ramadan and Jumu'ah cards. Off unless the employee turns it on. */
    private boolean islamicFeatures = false;

    /** City used for prayer times, e.g. "LAGOS". */
    private String city = "LAGOS";

    /** "STANDARD" (Shafi'i, Maliki, Hanbali) or "HANAFI" for the Asr time. */
    private String asrMethod = "STANDARD";

    private boolean taskReminders = true;
    private boolean prayerReminders = true;
    /** Jumu'ah reminder on Fridays, and suhoor / iftar alerts during Ramadan. */
    private boolean ramadanReminders = true;
    private boolean leaveAlerts = true;

    /** Which alert tune to play, e.g. "rose-chime". */
    private String tone = "rose-chime";

    /** Alert volume, 0 to 100. */
    private int volume = 70;

    public static UserSettings defaultsFor(String employeeId) {
        UserSettings s = new UserSettings();
        s.setEmployeeId(employeeId);
        return s;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public boolean isIslamicFeatures() { return islamicFeatures; }
    public void setIslamicFeatures(boolean islamicFeatures) { this.islamicFeatures = islamicFeatures; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAsrMethod() { return asrMethod; }
    public void setAsrMethod(String asrMethod) { this.asrMethod = asrMethod; }

    public boolean isTaskReminders() { return taskReminders; }
    public void setTaskReminders(boolean taskReminders) { this.taskReminders = taskReminders; }

    public boolean isPrayerReminders() { return prayerReminders; }
    public void setPrayerReminders(boolean prayerReminders) { this.prayerReminders = prayerReminders; }

    public boolean isRamadanReminders() { return ramadanReminders; }
    public void setRamadanReminders(boolean ramadanReminders) { this.ramadanReminders = ramadanReminders; }

    public boolean isLeaveAlerts() { return leaveAlerts; }
    public void setLeaveAlerts(boolean leaveAlerts) { this.leaveAlerts = leaveAlerts; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }
}