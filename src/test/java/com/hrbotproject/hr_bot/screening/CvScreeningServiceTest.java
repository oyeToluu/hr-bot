package com.hrbotproject.hr_bot.screening;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.AiScore;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.KeywordScore;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.ScreeningResult;
import com.hrbotproject.hr_bot.model.JobPosting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CvScreeningServiceTest {

    private final JobPosting job = JobPosting.builder().title("Dev").build();
    private final KeywordScore keyword = new KeywordScore(60, List.of(), List.of(), List.of(), 0, true, "kw");

    @Test
    @DisplayName("Without the AI model, the keyword score is the final score")
    void keywordOnly_whenAiOff() {
        KeywordCvScorer scorer = mock(KeywordCvScorer.class);
        AiCvScreener ai = mock(AiCvScreener.class);
        when(scorer.score(anyString(), any())).thenReturn(keyword);
        when(ai.screen(anyString(), any())).thenReturn(Optional.empty());

        ScreeningResult result = new CvScreeningService(scorer, ai).screen("cv", job);

        assertThat(result.ai()).isNull();
        assertThat(result.finalScore()).isEqualTo(60);
    }

    @Test
    @DisplayName("With the AI model, the final score is the average of both")
    void averagesKeywordAndAi() {
        KeywordCvScorer scorer = mock(KeywordCvScorer.class);
        AiCvScreener ai = mock(AiCvScreener.class);
        when(scorer.score(anyString(), any())).thenReturn(keyword);
        when(ai.screen(anyString(), any())).thenReturn(Optional.of(new AiScore(81, "ai")));

        ScreeningResult result = new CvScreeningService(scorer, ai).screen("cv", job);

        assertThat(result.finalScore()).isEqualTo(71); // (60 + 81) / 2 = 70.5 -> 71
    }

    @Test
    @DisplayName("AI screener is inactive without an API key, and never calls out")
    void aiScreener_offWithoutKey() {
        AiCvScreener screener = new AiCvScreener(true, "", "model", "https://example.invalid",
                JsonMapper.builder().build());

        assertThat(screener.isActive()).isFalse();
        assertThat(screener.screen("cv", job)).isEmpty();
    }

    @Test
    @DisplayName("AI reply is parsed even when wrapped in extra text, and clamped to 0-100")
    void aiReply_isParsedAndClamped() {
        AiCvScreener screener = new AiCvScreener(false, "", "model", "https://example.invalid",
                JsonMapper.builder().build());

        AiScore parsed = screener.parseReply("Here you go:\n{\"score\": 140, \"summary\": \"Great fit\"}");

        assertThat(parsed.score()).isEqualTo(100);
        assertThat(parsed.summary()).isEqualTo("Great fit");
        assertThat(screener.parseReply("no json here")).isNull();
    }
}
