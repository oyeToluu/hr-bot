package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.PolicyDtos.AskResponse;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.PolicyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyDocumentRepository);
    }

    private PolicyDocument handbook() {
        return PolicyDocument.builder()
                .id("doc-1")
                .title("Employee Handbook 2026")
                .sections(List.of(
                        PolicyDocument.Section.builder()
                                .heading("Leave Policy")
                                .content("Employees accrue 20 annual leave days per year.")
                                .keywords(List.of("leave", "annual", "vacation", "days"))
                                .build(),
                        PolicyDocument.Section.builder()
                                .heading("Remote Work Policy")
                                .content("Remote work is allowed up to 2 days per week after probation.")
                                .keywords(List.of("remote", "wfh", "hybrid"))
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("ask() returns the section with the most keyword overlap")
    void ask_returnsBestMatchingSection() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));

        AskResponse response = policyService.ask("How many annual leave days do I get?");

        assertThat(response.matches()).isNotEmpty();
        assertThat(response.matches().get(0).heading()).isEqualTo("Leave Policy");
        assertThat(response.answer()).contains("20 annual leave days");
    }

    @Test
    @DisplayName("ask() returns a fallback message when no section matches")
    void ask_returnsFallbackWhenNoMatch() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));

        AskResponse response = policyService.ask("What is the dress code?");

        assertThat(response.matches()).isEmpty();
        assertThat(response.answer()).contains("couldn't find a matching policy section");
    }

    @Test
    @DisplayName("ask() caps results at the top 3 matching sections")
    void ask_capsResultsAtThree() {
        PolicyDocument doc = PolicyDocument.builder()
                .id("doc-2")
                .title("Big Handbook")
                .sections(List.of(
                        section("A", "leave"), section("B", "leave"),
                        section("C", "leave"), section("D", "leave")
                ))
                .build();
        when(policyDocumentRepository.findAll()).thenReturn(List.of(doc));

        AskResponse response = policyService.ask("leave policy question");

        assertThat(response.matches()).hasSize(3);
    }

    private PolicyDocument.Section section(String heading, String keyword) {
        return PolicyDocument.Section.builder()
                .heading(heading)
                .content(heading + " content")
                .keywords(List.of(keyword))
                .build();
    }
}