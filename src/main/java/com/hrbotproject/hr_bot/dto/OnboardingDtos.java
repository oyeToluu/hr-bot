package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.OnboardingFaq;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class OnboardingDtos {

    public record AskRequest(@NotBlank String message) {}

    public record AskResponse(String answer, String matchedQuestion, int confidence) {}

    public record CreateFaqRequest(
            @NotBlank String question,
            @NotBlank String answer,
            List<String> keywords,
            OnboardingFaq.Category category
    ) {}
}
