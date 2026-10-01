package com.ejoobi.shortlist_copilot.model;

import java.util.List;

public record Explanation(
        String summary,
        List<String> strengths,
        List<String> gaps,
        List<String> interviewQuestions,
        String candidateMessage) {
}