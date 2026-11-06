package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Company;

import java.time.LocalDateTime;
import java.util.List;

public class OwnerDtos {

    public record OwnerCheck(boolean owner) {}

    public record HrContact(String name, String email) {}

    /**
     * What the App Owner sees about a company: a summary only. No employees'
     * private records (leave, policies, CVs, help requests) are included.
     */
    public record CompanySummary(
            String id,
            String name,
            String slug,
            String industry,
            Company.Plan plan,
            LocalDateTime trialEndsAt,
            LocalDateTime createdAt,
            long staffCount,
            long openJobs,
            List<HrContact> hrAdmins
    ) {}

    public record OwnerOverview(long companies, long onTrial, long paid, long totalStaff, List<CompanySummary> list) {}
}