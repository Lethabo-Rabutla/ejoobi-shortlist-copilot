package com.ejoobi.shortlist_copilot.model;

public record EvaluationResponse(
        CandidateProfile candidate,
        ScoreBreakdown score,
        Explanation explanation) {
}