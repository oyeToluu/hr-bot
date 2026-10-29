package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer;
import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer.AiAnswer;
import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer.PolicySection;
import com.hrbotproject.hr_bot.dto.PolicyDtos.AskResponse;
import com.hrbotproject.hr_bot.dto.PolicyDtos.MatchResult;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.PolicyDocumentRepository;
import com.hrbotproject.hr_bot.util.KeywordMatcher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PolicyService {

    /** If all policies together are longer than this, only the best keyword matches are sent to the AI. */
    static final int MAX_AI_CONTEXT_CHARS = 40_000;
    static final int MAX_AI_SECTIONS_WHEN_LARGE = 15;

    static final String NO_MATCH_ANSWER =
            "I couldn't find a matching policy section. Try rephrasing, or check with HR directly.";

    private final PolicyDocumentRepository policyDocumentRepository;
    private final AiPolicyAnswerer aiPolicyAnswerer;

    public PolicyService(PolicyDocumentRepository policyDocumentRepository, AiPolicyAnswerer aiPolicyAnswerer) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.aiPolicyAnswerer = aiPolicyAnswerer;
    }

    /** A policy section with its keyword score for the current question. */
    private record ScoredSection(int order, int score, String docTitle, String heading, String content) {
        MatchResult toMatch() {
            return new MatchResult(score, docTitle, heading, content);
        }
    }

    public AskResponse ask(String question) {
        List<ScoredSection> all = scoreAllSections(question);

        if (aiPolicyAnswerer.isActive()) {
            Optional<AskResponse> ai = askAi(question, all);
            if (ai.isPresent()) {
                return ai.get();
            }
        }
        return keywordAnswer(all);
    }

    // ---------- AI answer ----------

    private Optional<AskResponse> askAi(String question, List<ScoredSection> all) {
        List<ScoredSection> context = chooseAiContext(all);
        if (context.isEmpty()) {
            return Optional.empty();
        }

        List<PolicySection> numbered = new ArrayList<>();
        for (int i = 0; i < context.size(); i++) {
            ScoredSection s = context.get(i);
            numbered.add(new PolicySection(i + 1, s.docTitle(), s.heading(), s.content()));
        }

        Optional<AiAnswer> reply = aiPolicyAnswerer.answer(question, numbered);
        if (reply.isEmpty()) {
            return Optional.empty();
        }

        AiAnswer ai = reply.get();
        List<MatchResult> sources = new ArrayList<>();
        if (ai.found()) {
            for (Integer n : ai.sources()) {
                if (n != null && n >= 1 && n <= context.size()) {
                    MatchResult m = context.get(n - 1).toMatch();
                    if (!sources.contains(m)) sources.add(m);
                }
            }
        }
        return Optional.of(new AskResponse(ai.answer(), sources, true));
    }

    /**
     * Small policy sets go to the AI in full, so any wording works.
     * Very large ones are cut down to the best keyword matches first.
     */
    static List<ScoredSection> chooseAiContext(List<ScoredSection> all) {
        int totalChars = all.stream().mapToInt(s -> s.content().length() + s.heading().length()).sum();
        if (totalChars <= MAX_AI_CONTEXT_CHARS) {
            return all;
        }
        return all.stream()
                .sorted(Comparator.comparingInt(ScoredSection::score).reversed()
                        .thenComparingInt(ScoredSection::order))
                .limit(MAX_AI_SECTIONS_WHEN_LARGE)
                .toList();
    }

    // ---------- Keyword answer (fallback, and the behaviour when no API key is set) ----------

    private AskResponse keywordAnswer(List<ScoredSection> all) {
        List<MatchResult> top = all.stream()
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingInt(ScoredSection::score).reversed()
                        .thenComparingInt(ScoredSection::order))
                .limit(3)
                .map(ScoredSection::toMatch)
                .toList();

        if (top.isEmpty()) {
            return new AskResponse(NO_MATCH_ANSWER, List.of(), false);
        }
        return new AskResponse(top.get(0).content(), top, false);
    }

    private List<ScoredSection> scoreAllSections(String question) {
        Set<String> queryWords = KeywordMatcher.extractWords(question);
        List<ScoredSection> all = new ArrayList<>();
        int order = 0;

        for (PolicyDocument doc : policyDocumentRepository.findAll()) {
            if (doc.getSections() == null) continue;
            for (PolicyDocument.Section section : doc.getSections()) {
                Set<String> bag = new HashSet<>(section.getKeywords() == null ? List.of() : section.getKeywords());
                bag.addAll(KeywordMatcher.extractWords(section.getHeading()));

                int score = 0;
                for (String w : queryWords) {
                    if (bag.contains(w)) score++;
                }
                all.add(new ScoredSection(order++, score, doc.getTitle(), section.getHeading(), section.getContent()));
            }
        }
        return all;
    }

    public PolicyDocument createDocument(PolicyDocument document) {
        return policyDocumentRepository.save(document);
    }

    public List<PolicyDocument> listDocuments() {
        return policyDocumentRepository.findAll();
    }
}