package com.hrbotproject.hr_bot.screening;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.KeywordScore;
import com.hrbotproject.hr_bot.model.JobPosting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordCvScorerTest {

    private final KeywordCvScorer scorer = new KeywordCvScorer();

    private final JobPosting backendJob = JobPosting.builder()
            .title("Junior Backend Developer")
            .requiredSkills(List.of("Java", "Spring Boot", "REST API", "MongoDB", "Git"))
            .niceToHaveSkills(List.of("Docker", "JUnit", "React"))
            .minYearsExperience(2)
            .educationKeywords(List.of("BSc", "HND", "Computer Science"))
            .build();

    @Test
    @DisplayName("A strong CV that has every requirement scores 100")
    void strongCv_scoresFullMarks() {
        String cv = """
                Chidi Eze - Backend Developer
                BSc Computer Science, University of Lagos
                3 years building RESTful APIs with Java and Spring Boot.
                Databases: MongoDB, PostgreSQL. Tools: Git, Docker, JUnit, React.
                """;

        KeywordScore result = scorer.score(cv, backendJob);

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.missingSkills()).isEmpty();
        assertThat(result.matchedSkills()).containsExactly("Java", "Spring Boot", "REST API", "MongoDB", "Git");
        assertThat(result.bonusSkills()).containsExactly("Docker", "JUnit", "React");
        assertThat(result.yearsExperience()).isEqualTo(3);
        assertThat(result.educationMatched()).isTrue();
    }

    @Test
    @DisplayName("A CV with no matching skills scores low and lists what is missing")
    void weakCv_scoresLow() {
        String cv = "Fashion designer. 1 year of tailoring and sales experience. SSCE.";

        KeywordScore result = scorer.score(cv, backendJob);

        assertThat(result.score()).isLessThan(40);
        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.missingSkills()).hasSize(5);
        assertThat(result.summary()).contains("Has 0 of 5 required skills");
    }

    @Test
    @DisplayName("Skills are matched as whole words, not inside other words")
    void skills_matchWholeWordsOnly() {
        // "javascript" must not count as "java"; "legit" must not count as "git"
        String cv = "javascript developer. legit work ethic.";

        KeywordScore result = scorer.score(cv, backendJob);

        assertThat(result.matchedSkills()).doesNotContain("Java", "Git");
    }

    @Test
    @DisplayName("Common aliases count: 'springboot', 'mongo', 'restful'")
    void aliases_areRecognised() {
        String cv = "Built restful services in springboot backed by mongo.";

        KeywordScore result = scorer.score(cv, backendJob);

        assertThat(result.matchedSkills()).contains("Spring Boot", "MongoDB", "REST API");
    }

    @Test
    @DisplayName("Experience is read from work-history date ranges, overlaps counted once")
    void experience_fromDateRanges() {
        int thisYear = Year.now().getValue();
        String cv = "Software Intern, Andela 2018 - 2019\n"
                + "Backend Developer, Paystack 2019 – present\n"
                + "Freelance 2020 - 2021"; // overlaps the job above

        int years = scorer.detectYearsOfExperience(KeywordCvScorer.normalise(cv));

        assertThat(years).isEqualTo(thisYear - 2018);
    }

    @Test
    @DisplayName("A job with no requirements gives full marks for those parts")
    void emptyRequirements_giveFullMarks() {
        JobPosting open = JobPosting.builder().title("Anything").build();

        KeywordScore result = scorer.score("Any CV text at all", open);

        assertThat(result.score()).isEqualTo(100);
    }
}
