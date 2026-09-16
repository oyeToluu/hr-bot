package com.hrbotproject.hr_bot.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_faqs")
public class OnboardingFaq {

    @Id
    private String id;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    @Builder.Default
    private List<String> keywords = List.of();

    public enum Category { PAYROLL, BENEFITS, EQUIPMENT, WORKPLACE, GENERAL }

    @Builder.Default
    private Category category = Category.GENERAL;
}
