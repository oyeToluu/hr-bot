package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.OnboardingDtos.AskResponse;
import com.hrbotproject.hr_bot.dto.OnboardingDtos.CreateFaqRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.OnboardingFaq;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.OnboardingFaqRepository;
import com.hrbotproject.hr_bot.util.KeywordMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingFaqRepository faqRepository;
    private final EmployeeRepository employeeRepository;

    // MVP note: keyword-overlap matching (stopwords filtered via
    // KeywordMatcher - see that class for why that matters), same approach
    // as the Policy Q&A assistant. A future upgrade could swap this for
    // embeddings-based semantic search.
    public AskResponse ask(String message) {
        Set<String> queryWords = KeywordMatcher.extractWords(message);

        OnboardingFaq best = null;
        int bestScore = 0;

        for (OnboardingFaq faq : faqRepository.findAll()) {
            Set<String> bag = new HashSet<>(faq.getKeywords());
            bag.addAll(KeywordMatcher.extractWords(faq.getQuestion()));

            int score = 0;
            for (String w : queryWords) {
                if (bag.contains(w)) score++;
            }

            if (score > bestScore) {
                bestScore = score;
                best = faq;
            }
        }

        if (best != null && bestScore > 0) {
            return new AskResponse(best.getAnswer(), best.getQuestion(), bestScore);
        }

        return new AskResponse(
                "I don't have an answer for that yet. Try asking about payroll, benefits, "
                        + "equipment, or workplace basics - or contact HR directly.",
                null,
                0
        );
    }

    public List<OnboardingFaq> listFaqs() {
        return faqRepository.findAll();
    }

    public OnboardingFaq createFaq(CreateFaqRequest req) {
        OnboardingFaq faq = OnboardingFaq.builder()
                .question(req.question())
                .answer(req.answer())
                .keywords(req.keywords() == null ? List.of() : req.keywords())
                .category(req.category() == null ? OnboardingFaq.Category.GENERAL : req.category())
                .build();
        return faqRepository.save(faq);
    }

    public Employee markOnboardingComplete(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));
        employee.setOnboardingComplete(true);
        return employeeRepository.save(employee);
    }
}