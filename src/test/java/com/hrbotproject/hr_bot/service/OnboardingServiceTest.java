package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.OnboardingDtos.AskResponse;
import com.hrbotproject.hr_bot.dto.OnboardingDtos.CreateFaqRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.OnboardingFaq;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.OnboardingFaqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private OnboardingFaqRepository faqRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private OnboardingService onboardingService;

    private final OnboardingFaq payrollFaq = OnboardingFaq.builder()
            .id("faq-1")
            .question("When do I get paid?")
            .answer("Salaries are paid on the last working day of every month.")
            .keywords(List.of("pay", "paid", "payroll", "salary"))
            .category(OnboardingFaq.Category.PAYROLL)
            .build();

    private final OnboardingFaq equipmentFaq = OnboardingFaq.builder()
            .id("faq-2")
            .question("What equipment will I be issued?")
            .answer("A laptop, monitor, and access badge within 3 working days.")
            .keywords(List.of("laptop", "equipment", "device", "badge"))
            .category(OnboardingFaq.Category.EQUIPMENT)
            .build();

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(faqRepository, employeeRepository);
    }

    @Test
    @DisplayName("ask() returns the FAQ with the most keyword overlap")
    void ask_returnsBestKeywordMatch() {
        when(faqRepository.findAll()).thenReturn(List.of(payrollFaq, equipmentFaq));

        AskResponse response = onboardingService.ask("When will I get my salary paid?");

        assertThat(response.matchedQuestion()).isEqualTo("When do I get paid?");
        assertThat(response.confidence()).isGreaterThan(0);
        assertThat(response.answer()).contains("last working day");
    }

    @Test
    @DisplayName("ask() falls back to a default answer when nothing matches")
    void ask_returnsFallbackWhenNoMatch() {
        when(faqRepository.findAll()).thenReturn(List.of(payrollFaq, equipmentFaq));

        AskResponse response = onboardingService.ask("What is the meaning of life?");

        assertThat(response.matchedQuestion()).isNull();
        assertThat(response.confidence()).isZero();
        assertThat(response.answer()).contains("don't have an answer");
    }

    @Test
    @DisplayName("createFaq() saves a new FAQ with defaults applied")
    void createFaq_appliesDefaults() {
        CreateFaqRequest req = new CreateFaqRequest("New question?", "New answer.", null, null);
        when(faqRepository.save(any(OnboardingFaq.class))).thenAnswer(inv -> inv.getArgument(0));

        OnboardingFaq saved = onboardingService.createFaq(req);

        assertThat(saved.getCategory()).isEqualTo(OnboardingFaq.Category.GENERAL);
        assertThat(saved.getKeywords()).isEmpty();
    }

    @Test
    @DisplayName("markOnboardingComplete() flips the flag and saves")
    void markOnboardingComplete_updatesEmployee() {
        Employee employee = Employee.builder().id("emp-1").fullName("Ada").email("ada@hrbots.test")
                .onboardingComplete(false).build();
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = onboardingService.markOnboardingComplete("emp-1");

        assertThat(result.isOnboardingComplete()).isTrue();
    }

    @Test
    @DisplayName("markOnboardingComplete() throws for an unknown employee")
    void markOnboardingComplete_throwsWhenEmployeeMissing() {
        when(employeeRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.markOnboardingComplete("ghost"))
                .isInstanceOf(NoSuchElementException.class);
    }
}