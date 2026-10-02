package com.ejoobi.shortlist_copilot.service;

import com.ejoobi.shortlist_copilot.model.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Service
public class ExplanationService {

    private static final String SYSTEM_PROMPT = """
            You are an assistant for recruiters. You receive a job, a candidate profile and a
            score breakdown that was ALREADY calculated. Do not recalculate or change any score.
            Reply with JSON only, using exactly these keys:
            summary (string, two sentences),
            strengths (array of strings),
            gaps (array of strings),
            interviewQuestions (array of exactly 5 strings that probe the gaps),
            candidateMessage (string: a short, polite draft update for the candidate that
            does not mention the score and does not reject them).
            Use only the data provided. Never mention or infer age, gender, race or other
            personal traits. n the candidate data, willingToRelocate=false only means the CV does not say.
            Never state or imply that the candidate is unwilling to relocate.
            If the location score is the full 20, do not list location as a gap
            and do not ask about relocation. A human recruiter makes the final decision.
            """;

    private final AzureOpenAiClient aiClient;
    private final JsonMapper jsonMapper;

    public ExplanationService(AzureOpenAiClient aiClient, JsonMapper jsonMapper) {
        this.aiClient = aiClient;
        this.jsonMapper = jsonMapper;
    }

    public Explanation explain(JobRequirements job, CandidateProfile candidate, ScoreBreakdown score) {
        String input = jsonMapper.writeValueAsString(
                Map.of("job", job, "candidate", candidate, "score", score));
        String content = aiClient.chatJson(SYSTEM_PROMPT, input);
        try {
            Explanation raw = jsonMapper.readValue(content, Explanation.class);
            return new Explanation(
                    raw.summary() == null ? "" : raw.summary(),
                    raw.strengths() == null ? List.of() : raw.strengths(),
                    raw.gaps() == null ? List.of() : raw.gaps(),
                    raw.interviewQuestions() == null ? List.of() : raw.interviewQuestions(),
                    raw.candidateMessage() == null ? "" : raw.candidateMessage());
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not read the AI model's explanation", e);
        }
    }
}