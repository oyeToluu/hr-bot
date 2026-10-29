package com.hrbotproject.hr_bot.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class PolicyDtos {

    public record AskRequest(@NotBlank String question) {}

    public record MatchResult(int score, String docTitle, String heading, String content) {}

public record AskResponse(String answer, List<MatchResult> matches, boolean aiAnswer) {}}
