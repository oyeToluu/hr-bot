package com.hrbotproject.hr_bot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A business that uses HR Bots. Every employee, leave request, policy, FAQ,
 * job and application belongs to exactly one company, and HR admins only
 * ever see their own company's records.
 */
@Document(collection = "companies")
public class Company {

    public enum Plan { FREE_TRIAL, PAID }

    @Id
    private String id;

    private String name;

    /** Short web-friendly name used in links, e.g. "zenith-foods-ltd". */
    @Indexed(unique = true)
    private String slug;

    private String industry;

    private Plan plan = Plan.FREE_TRIAL;
    private LocalDateTime trialEndsAt;

    /**
     * The secret part of the "join your company" link HR shares with staff.
     * Only HR admins can see it; it can be changed if it leaks.
     */
    @JsonIgnore
    @Indexed(unique = true)
    private String joinCode;

    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(LocalDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}