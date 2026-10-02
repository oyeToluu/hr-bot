package com.hrbotproject.hr_bot.screening;

import com.hrbotproject.hr_bot.dto.RecruitmentDtos.KeywordScore;
import com.hrbotproject.hr_bot.model.JobPosting;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rule-based half of the screening bot. Always runs, needs no internet
 * and no API key, and every point can be explained:
 *
 *   required skills found    60 pts  (share of the job's must-have skills)
 *   nice-to-have skills      15 pts
 *   years of experience      15 pts  (detected vs the job's minimum)
 *   education keywords       10 pts
 *                           -------
 *                           100 pts
 */
@Component
public class KeywordCvScorer {

    static final int REQUIRED_POINTS = 60;
    static final int BONUS_POINTS = 15;
    static final int EXPERIENCE_POINTS = 15;
    static final int EDUCATION_POINTS = 10;

    // Other ways the same skill is commonly written on CVs.
    private static final Map<String, List<String>> ALIASES = Map.ofEntries(
            Map.entry("javascript", List.of("js", "ecmascript")),
            Map.entry("typescript", List.of("ts")),
            Map.entry("react", List.of("react.js", "reactjs")),
            Map.entry("node.js", List.of("node", "nodejs")),
            Map.entry("spring boot", List.of("springboot", "spring-boot")),
            Map.entry("mongodb", List.of("mongo")),
            Map.entry("postgresql", List.of("postgres")),
            Map.entry("rest api", List.of("restful", "rest apis", "restful api", "restful apis")),
            Map.entry("machine learning", List.of("ml")),
            Map.entry("microsoft excel", List.of("excel", "ms excel")),
            Map.entry("excel", List.of("microsoft excel", "ms excel")),
            Map.entry("human resources", List.of("hr")),
            Map.entry("customer service", List.of("customer support", "client service")),
            Map.entry("project management", List.of("project manager", "pmp")),
            Map.entry("docker", List.of("docker compose", "dockerfile")),
            Map.entry("git", List.of("github", "gitlab"))
    );

    // "5 years", "5+ years", "3 yrs", "10 years'"
    private static final Pattern YEARS_STATED =
            Pattern.compile("(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)\\b");

    // "2019 - 2023", "2020 – present", "2018 to date"
    private static final Pattern YEAR_RANGE = Pattern.compile(
            "\\b((?:19|20)\\d{2})\\s*(?:-|–|—|to)\\s*((?:19|20)\\d{2}|present|current|date|now)\\b");

    public KeywordScore score(String cvText, JobPosting job) {
        String text = normalise(cvText);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : job.getRequiredSkills()) {
            (mentions(text, skill) ? matched : missing).add(skill);
        }

        List<String> bonus = new ArrayList<>();
        for (String skill : job.getNiceToHaveSkills()) {
            if (mentions(text, skill)) bonus.add(skill);
        }

        int years = detectYearsOfExperience(text);
        boolean education = job.getEducationKeywords().isEmpty()
                || job.getEducationKeywords().stream().anyMatch(k -> mentions(text, k));

        double requiredPart = job.getRequiredSkills().isEmpty()
                ? REQUIRED_POINTS
                : REQUIRED_POINTS * matched.size() / (double) job.getRequiredSkills().size();
        double bonusPart = job.getNiceToHaveSkills().isEmpty()
                ? BONUS_POINTS
                : BONUS_POINTS * bonus.size() / (double) job.getNiceToHaveSkills().size();
        double experiencePart = job.getMinYearsExperience() <= 0
                ? EXPERIENCE_POINTS
                : EXPERIENCE_POINTS * Math.min(1.0, years / (double) job.getMinYearsExperience());
        double educationPart = education ? EDUCATION_POINTS : 0;

        int score = (int) Math.round(requiredPart + bonusPart + experiencePart + educationPart);

        String summary = buildSummary(job, matched, missing, bonus, years, education);
        return new KeywordScore(score, matched, missing, bonus, years, education, summary);
    }

    /** True if the skill (or a known alias) appears as a whole word/phrase. */
    boolean mentions(String normalisedText, String skill) {
        String key = skill.toLowerCase(Locale.ROOT).trim();
        if (key.isEmpty()) return false;
        if (containsTerm(normalisedText, key)) return true;
        for (String alias : ALIASES.getOrDefault(key, List.of())) {
            if (containsTerm(normalisedText, alias)) return true;
        }
        return false;
    }

    // Whole-term match that still works for "c++", "c#", "node.js":
    // the term must not be glued to other letters/digits on either side.
    private static boolean containsTerm(String text, String term) {
        Pattern p = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])");
        return p.matcher(text).find();
    }

    /**
     * Takes the larger of (a) the biggest "N years" the CV states and
     * (b) the total length of the date ranges in its work history,
     * with overlapping ranges merged so two jobs held at once count once.
     */
    int detectYearsOfExperience(String text) {
        int stated = 0;
        Matcher m = YEARS_STATED.matcher(text);
        while (m.find()) {
            stated = Math.max(stated, Integer.parseInt(m.group(1)));
        }

        int thisYear = Year.now().getValue();
        List<int[]> ranges = new ArrayList<>();
        Matcher r = YEAR_RANGE.matcher(text);
        while (r.find()) {
            int start = Integer.parseInt(r.group(1));
            String endText = r.group(2);
            int end = Character.isDigit(endText.charAt(0)) ? Integer.parseInt(endText) : thisYear;
            if (start <= end && end <= thisYear) {
                ranges.add(new int[]{start, end});
            }
        }
        ranges.sort(Comparator.comparingInt(a -> a[0]));

        int fromRanges = 0;
        int curStart = -1;
        int curEnd = -1;
        for (int[] range : ranges) {
            if (curStart < 0) {
                curStart = range[0];
                curEnd = range[1];
            } else if (range[0] <= curEnd) {
                curEnd = Math.max(curEnd, range[1]);
            } else {
                fromRanges += curEnd - curStart;
                curStart = range[0];
                curEnd = range[1];
            }
        }
        if (curStart >= 0) fromRanges += curEnd - curStart;

        return Math.min(40, Math.max(stated, fromRanges));
    }

    static String normalise(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String buildSummary(JobPosting job, List<String> matched, List<String> missing,
                                       List<String> bonus, int years, boolean education) {
        StringBuilder sb = new StringBuilder();
        if (job.getRequiredSkills().isEmpty()) {
            sb.append("No required skills set for this job. ");
        } else {
            sb.append("Has ").append(matched.size()).append(" of ")
                    .append(job.getRequiredSkills().size()).append(" required skills");
            sb.append(missing.isEmpty() ? ". " : " (missing: " + String.join(", ", missing) + "). ");
        }
        if (!bonus.isEmpty()) {
            sb.append("Bonus skills: ").append(String.join(", ", bonus)).append(". ");
        }
        sb.append("About ").append(years).append(years == 1 ? " year" : " years").append(" of experience detected");
        if (job.getMinYearsExperience() > 0) {
            sb.append(" (job asks for ").append(job.getMinYearsExperience()).append(")");
        }
        sb.append(". ");
        if (!job.getEducationKeywords().isEmpty()) {
            sb.append(education ? "Education requirement found." : "Education requirement not found.");
        }
        return sb.toString().trim();
    }
}
