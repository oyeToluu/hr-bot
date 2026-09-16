package com.hrbotproject.hr_bot.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared word-extraction for the keyword-matching bots (OnboardingService,
 * PolicyService). Filters common English function words ("what", "is",
 * "the", "will"...) before matching - without this, a query like "What is
 * the meaning of life?" can spuriously match any FAQ whose own question
 * happens to start with "What ...", since "what" alone would count as a
 * keyword overlap. Bug found by OnboardingServiceTest.ask_returnsFallbackWhenNoMatch.
 */
public final class KeywordMatcher {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z0-9]+");

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "but", "by",
            "can", "could", "did", "do", "does", "for", "from", "had", "has",
            "have", "how", "i", "if", "in", "into", "is", "it", "its", "me",
            "my", "of", "on", "or", "our", "should", "so", "that", "the",
            "their", "them", "then", "there", "these", "this", "those", "to",
            "up", "was", "we", "what", "when", "where", "which", "who",
            "whom", "why", "will", "with", "would", "you", "your", "yours"
    );

    private KeywordMatcher() {
    }

    /** Lowercased word tokens from text, with common stopwords removed. */
    public static Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();
        Matcher m = WORD_PATTERN.matcher(text.toLowerCase());
        while (m.find()) {
            String word = m.group();
            if (!STOPWORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }
}