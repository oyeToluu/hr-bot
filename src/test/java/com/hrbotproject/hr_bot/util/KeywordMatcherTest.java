package com.hrbotproject.hr_bot.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordMatcherTest {

    @Test
    void extractWords_lowercasesAndTokenizes() {
        Set<String> words = KeywordMatcher.extractWords("Laptop, Monitor, and Badge!");
        assertThat(words).contains("laptop", "monitor", "badge");
    }

    @Test
    void extractWords_removesCommonStopwords() {
        Set<String> words = KeywordMatcher.extractWords("What is the meaning of life?");
        assertThat(words).doesNotContain("what", "is", "the", "of");
        assertThat(words).contains("meaning", "life");
    }

    @Test
    void extractWords_keepsContentWordsThatLookLikeStopwordsInContext() {
        Set<String> words = KeywordMatcher.extractWords("When do I get paid?");
        assertThat(words).contains("get", "paid");
        assertThat(words).doesNotContain("when", "do", "i");
    }
}