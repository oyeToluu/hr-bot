package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer;
import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer.AiAnswer;
import com.hrbotproject.hr_bot.dto.PolicyDtos.AskResponse;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.LeaveRequestRepository;
import com.hrbotproject.hr_bot.repository.PolicyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    // A mock returns false for isActive() unless told otherwise,
    // so the keyword tests below run exactly as before (no API key).
    @Mock
    private AiPolicyAnswerer aiPolicyAnswerer;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyDocumentRepository, leaveRequestRepository, aiPolicyAnswerer);
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

    // ---------- Keyword matching (no API key) ----------

    @Test
    @DisplayName("ask() returns the section with the most keyword overlap")
    void ask_returnsBestMatchingSection() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));

        AskResponse response = policyService.ask("How many annual leave days do I get?");

        assertThat(response.matches()).isNotEmpty();
        assertThat(response.matches().get(0).heading()).isEqualTo("Leave Policy");
        assertThat(response.answer()).contains("20 annual leave days");
        assertThat(response.aiAnswer()).isFalse();
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

    // ---------- AI answers (API key set) ----------

    @Test
    @DisplayName("ask() uses the AI answer and the sections it cites as sources")
    void ask_usesAiAnswerWithSources() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));
        when(aiPolicyAnswerer.isActive()).thenReturn(true);
        // Section 2 is Remote Work Policy (sections are numbered in order)
        when(aiPolicyAnswerer.answer(anyString(), anyList(), any())).thenReturn(Optional.of(
                new AiAnswer(true, "Yes, up to 2 days a week once your probation ends.", List.of(2), false)));

        // Worded with no keywords at all - the AI still gets every section
        AskResponse response = policyService.ask("Am I allowed to do my job from my house?");

        assertThat(response.aiAnswer()).isTrue();
        assertThat(response.answer()).isEqualTo("Yes, up to 2 days a week once your probation ends.");
        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).heading()).isEqualTo("Remote Work Policy");
    }

    @Test
    @DisplayName("ask() shows no sources when the AI says the policies don't cover it")
    void ask_aiNotFoundHasNoSources() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));
        when(aiPolicyAnswerer.isActive()).thenReturn(true);
        when(aiPolicyAnswerer.answer(anyString(), anyList(), any())).thenReturn(Optional.of(
                new AiAnswer(false, "The company policies don't cover the dress code. Please contact HR.", List.of(1), false)));

        AskResponse response = policyService.ask("What is the dress code?");

        assertThat(response.aiAnswer()).isTrue();
        assertThat(response.matches()).isEmpty();
        assertThat(response.answer()).contains("contact HR");
    }

    @Test
    @DisplayName("ask() ignores source numbers that don't exist")
    void ask_ignoresInvalidSourceNumbers() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));
        when(aiPolicyAnswerer.isActive()).thenReturn(true);
        when(aiPolicyAnswerer.answer(anyString(), anyList(), any())).thenReturn(Optional.of(
                new AiAnswer(true, "You get 20 annual leave days.", List.of(1, 1, 7, 0), false)));

        AskResponse response = policyService.ask("leave days?");

        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).heading()).isEqualTo("Leave Policy");
    }

    @Test
    @DisplayName("ask() falls back to keyword matching when the AI call fails")
    void ask_fallsBackWhenAiFails() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));
        when(aiPolicyAnswerer.isActive()).thenReturn(true);
        when(aiPolicyAnswerer.answer(anyString(), anyList(), any())).thenReturn(Optional.empty());

        AskResponse response = policyService.ask("How many annual leave days do I get?");

        assertThat(response.aiAnswer()).isFalse();
        assertThat(response.answer()).contains("20 annual leave days");
    }

    // ---------- Personal questions (the asker's own record) ----------

    private Employee ada() {
        return Employee.builder()
                .id("emp-1")
                .fullName("Ada Okafor")
                .email("ada@hrbots.test")
                .password("$2a$10$secret-hash")
                .department("Engineering")
                .role(Employee.Role.EMPLOYEE)
                .dateStarted(LocalDate.of(2026, 10, 1))
                .leaveBalance(15)
                .build();
    }

    @Test
    @DisplayName("ask() gives the AI only the asker's own record, without password or email")
    void ask_sendsOnlyOwnRecordToAi() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));
        when(aiPolicyAnswerer.isActive()).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc("emp-1")).thenReturn(List.of(
                LeaveRequest.builder().employeeId("emp-1").leaveType(LeaveRequest.LeaveType.ANNUAL)
                        .startDate(LocalDate.of(2026, 11, 3)).endDate(LocalDate.of(2026, 11, 7))
                        .daysRequested(5).status(LeaveRequest.Status.PENDING).build()));
        when(aiPolicyAnswerer.answer(anyString(), anyList(), any())).thenReturn(Optional.of(
                new AiAnswer(true, "You have 15 leave days left.", List.of(), true)));

        AskResponse response = policyService.ask("How many leave days do I have left?", ada());

        ArgumentCaptor<String> record = ArgumentCaptor.forClass(String.class);
        verify(aiPolicyAnswerer).answer(eq("How many leave days do I have left?"), anyList(), record.capture());
        assertThat(record.getValue())
                .contains("Ada Okafor", "Leave days remaining: 15", "ANNUAL, 2026-11-03 to 2026-11-07", "PENDING")
                .doesNotContain("secret-hash", "ada@hrbots.test", "emp-1");

        assertThat(response.answer()).isEqualTo("You have 15 leave days left.");
        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).docTitle()).isEqualTo("Your HR record");
    }

    @Test
    @DisplayName("ask() never shows 'Your HR record' as a source when nobody is logged in")
    void ask_noRecordSourceWithoutAsker() {
        when(policyDocumentRepository.findAll()).thenReturn(List.of(handbook()));
        when(aiPolicyAnswerer.isActive()).thenReturn(true);
        when(aiPolicyAnswerer.answer(anyString(), anyList(), any())).thenReturn(Optional.of(
                new AiAnswer(true, "You get 20 annual leave days.", List.of(1), true)));

        AskResponse response = policyService.ask("How many leave days do I get?");

        verify(aiPolicyAnswerer).answer(anyString(), anyList(), isNull());
        assertThat(response.matches()).extracting("docTitle").containsExactly("Employee Handbook 2026");
    }

    private PolicyDocument.Section section(String heading, String keyword) {
        return PolicyDocument.Section.builder()
                .heading(heading)
                .content(heading + " content")
                .keywords(List.of(keyword))
                .build();
    }
}