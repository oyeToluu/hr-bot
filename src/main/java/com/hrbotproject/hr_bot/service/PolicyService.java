package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer;
import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer.AiAnswer;
import com.hrbotproject.hr_bot.assistant.AiPolicyAnswerer.PolicySection;
import com.hrbotproject.hr_bot.dto.PolicyDtos.AskResponse;
import com.hrbotproject.hr_bot.dto.PolicyDtos.MatchResult;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.LeaveRequestRepository;
import com.hrbotproject.hr_bot.repository.PolicyDocumentRepository;
import com.hrbotproject.hr_bot.util.KeywordMatcher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    /** How many of the employee's most recent leave requests the AI gets to see. */
    static final int MAX_LEAVE_REQUESTS_IN_RECORD = 10;

    /** Shown as the source when an answer comes from the employee's own record. */
    static final MatchResult OWN_RECORD_SOURCE =
            new MatchResult(0, "Your HR record", "Personal details & leave", "");

    static final String NO_MATCH_ANSWER =
            "I couldn't find a matching policy section. Try rephrasing, or check with HR directly.";

    private final PolicyDocumentRepository policyDocumentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AiPolicyAnswerer aiPolicyAnswerer;

    public PolicyService(
            PolicyDocumentRepository policyDocumentRepository,
            LeaveRequestRepository leaveRequestRepository,
            AiPolicyAnswerer aiPolicyAnswerer
    ) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.aiPolicyAnswerer = aiPolicyAnswerer;
    }

    /** A policy section with its keyword score for the current question. */
    private record ScoredSection(int order, int score, String docTitle, String heading, String content) {
        MatchResult toMatch() {
            return new MatchResult(score, docTitle, heading, content);
        }
    }

    /** A question with no logged-in employee: policies only. */
    public AskResponse ask(String question) {
        return ask(question, null);
    }

    /**
     * @param asker the logged-in employee (from their token), or null. Only this
     *              employee's own record is ever given to the AI.
     */
    public AskResponse ask(String question, Employee asker) {
        List<ScoredSection> all = scoreAllSections(question);

        if (aiPolicyAnswerer.isActive()) {
            String record = asker == null ? null : buildEmployeeRecord(asker);
            Optional<AskResponse> ai = askAi(question, all, record);
            if (ai.isPresent()) {
                return ai.get();
            }
        }
        return keywordAnswer(all);
    }

    // ---------- AI answer ----------

    private Optional<AskResponse> askAi(String question, List<ScoredSection> all, String employeeRecord) {
        List<ScoredSection> context = chooseAiContext(all);
        if (context.isEmpty() && employeeRecord == null) {
            return Optional.empty();
        }

        List<PolicySection> numbered = new ArrayList<>();
        for (int i = 0; i < context.size(); i++) {
            ScoredSection s = context.get(i);
            numbered.add(new PolicySection(i + 1, s.docTitle(), s.heading(), s.content()));
        }

        Optional<AiAnswer> reply = aiPolicyAnswerer.answer(question, numbered, employeeRecord);
        if (reply.isEmpty()) {
            return Optional.empty();
        }

        AiAnswer ai = reply.get();
        List<MatchResult> sources = new ArrayList<>();
        if (ai.found() && ai.usedRecord() && employeeRecord != null) {
            sources.add(OWN_RECORD_SOURCE);
        }
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

    /**
     * The asking employee's own details as plain text for the AI. Deliberately
     * leaves out the password hash, email and database ids.
     */
    String buildEmployeeRecord(Employee e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Today's date: ").append(LocalDate.now()).append('\n');
        sb.append("Name: ").append(e.getFullName()).append('\n');
        sb.append("Department: ").append(e.getDepartment()).append('\n');
        sb.append("Role: ").append(e.getRole() == Employee.Role.HR_ADMIN ? "HR admin" : "Employee").append('\n');
        sb.append("Start date: ").append(e.getDateStarted()).append('\n');
        sb.append("Leave days remaining: ").append(formatDays(e.getLeaveBalance())).append('\n');
        sb.append("Onboarding complete: ").append(e.isOnboardingComplete() ? "yes" : "no").append('\n');

        List<LeaveRequest> requests = e.getId() == null ? List.of()
                : leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(e.getId());
        if (requests.isEmpty()) {
            sb.append("Leave requests: none\n");
        } else {
            sb.append("Leave requests (most recent first):\n");
            requests.stream().limit(MAX_LEAVE_REQUESTS_IN_RECORD).forEach(r -> {
                sb.append("- ").append(r.getLeaveType())
                  .append(", ").append(r.getStartDate()).append(" to ").append(r.getEndDate())
                  .append(", ").append(r.getDaysRequested()).append(" day(s)")
                  .append(", status: ").append(r.getStatus());
                if (r.getReviewNote() != null && !r.getReviewNote().isBlank()) {
                    sb.append(", HR note: ").append(r.getReviewNote());
                }
                sb.append('\n');
            });
        }
        return sb.toString();
    }

    private static String formatDays(double days) {
        return days == Math.rint(days) ? String.valueOf((long) days) : String.valueOf(days);
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