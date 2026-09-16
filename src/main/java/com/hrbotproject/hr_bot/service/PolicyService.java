package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.PolicyDtos.AskResponse;
import com.hrbotproject.hr_bot.dto.PolicyDtos.MatchResult;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.PolicyDocumentRepository;
import com.hrbotproject.hr_bot.util.KeywordMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyDocumentRepository policyDocumentRepository;

    public AskResponse ask(String question) {
        Set<String> queryWords = KeywordMatcher.extractWords(question);
        List<MatchResult> scored = new ArrayList<>();

        for (PolicyDocument doc : policyDocumentRepository.findAll()) {
            for (PolicyDocument.Section section : doc.getSections()) {
                Set<String> bag = new HashSet<>(section.getKeywords());
                bag.addAll(KeywordMatcher.extractWords(section.getHeading()));

                int score = 0;
                for (String w : queryWords) {
                    if (bag.contains(w)) score++;
                }

                if (score > 0) {
                    scored.add(new MatchResult(score, doc.getTitle(), section.getHeading(), section.getContent()));
                }
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        List<MatchResult> top = scored.size() > 3 ? scored.subList(0, 3) : scored;

        if (top.isEmpty()) {
            return new AskResponse(
                    "I couldn't find a matching policy section. Try rephrasing, or check with HR directly.",
                    List.of()
            );
        }

        return new AskResponse(top.get(0).content(), top);
    }

    public PolicyDocument createDocument(PolicyDocument document) {
        return policyDocumentRepository.save(document);
    }

    public List<PolicyDocument> listDocuments() {
        return policyDocumentRepository.findAll();
    }
}