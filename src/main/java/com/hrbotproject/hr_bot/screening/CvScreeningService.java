package com.hrbotproject.hr_bot.screening;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.AiScore;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.KeywordScore;
import com.hrbotproject.hr_bot.dto.RecruitmentDtos.ScreeningResult;
import com.hrbotproject.hr_bot.model.JobPosting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Runs both halves of the screening bot and combines them:
 *   - keyword score only, when the AI model is off or unavailable
 *   - the average of keyword and AI scores, when the AI model answers
 * Averaging keeps the transparent rule-based score in every decision,
 * so the AI alone can never push a CV over (or under) a threshold.
 */
@Service
@RequiredArgsConstructor
public class CvScreeningService {

    private final KeywordCvScorer keywordScorer;
    private final AiCvScreener aiScreener;

    public ScreeningResult screen(String cvText, JobPosting job) {
        KeywordScore keyword = keywordScorer.score(cvText, job);
        AiScore ai = aiScreener.screen(cvText, job).orElse(null);

        int finalScore = ai == null
                ? keyword.score()
                : (int) Math.round((keyword.score() + ai.score()) / 2.0);

        return new ScreeningResult(keyword, ai, finalScore);
    }
}
