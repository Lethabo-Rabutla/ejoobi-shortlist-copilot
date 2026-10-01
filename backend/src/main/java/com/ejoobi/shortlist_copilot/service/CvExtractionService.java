package com.ejoobi.shortlist_copilot.service;

import com.ejoobi.shortlist_copilot.model.CandidateProfile;
import com.ejoobi.shortlist_copilot.model.QualificationLevel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class CvExtractionService {

    private static final String SYSTEM_PROMPT = """
            You extract facts from CVs. Reply with JSON only, using exactly these keys:
            name (string), skills (array of strings), yearsExperience (number),
            city (string), willingToRelocate (boolean),
            highestQualificationLevel (one of NONE, CERTIFICATE, DIPLOMA, DEGREE, POSTGRADUATE).
            Use only information stated in the CV.
            Never infer age, gender, race or other personal traits.
            If something is not stated, use 0 for yearsExperience, an empty string for text,
            an empty array for skills, false for willingToRelocate, and NONE for highestQualificationLevel.
            """;

    private final AzureOpenAiClient aiClient;
    private final JsonMapper jsonMapper;

    public CvExtractionService(AzureOpenAiClient aiClient, JsonMapper jsonMapper) {
        this.aiClient = aiClient;
        this.jsonMapper = jsonMapper;
    }

    public CandidateProfile extract(String cvText) {
        String content = aiClient.chatJson(SYSTEM_PROMPT, "CV:\n" + cvText);
        try {
            CandidateProfile raw = jsonMapper.readValue(content, CandidateProfile.class);
            return new CandidateProfile(
                    raw.name(),
                    raw.skills() == null ? List.of() : raw.skills(),
                    raw.yearsExperience(),
                    raw.city(),
                    raw.willingToRelocate(),
                    raw.highestQualificationLevel() == null
                            ? QualificationLevel.NONE
                            : raw.highestQualificationLevel());
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not read the AI model's answer", e);
        }
    }
}