package com.ejoobi.shortlist_copilot.service;

import com.ejoobi.shortlist_copilot.model.CandidateProfile;
import com.ejoobi.shortlist_copilot.model.QualificationLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

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
            
    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String deployment;

    public CvExtractionService(
        JsonMapper jsonMapper,
        @Value ("${azure.openai.endpoint}") String endpoint,
        @Value ("${azure.openai.api-key}") String apiKey,
        @Value ("${azure.openai.deployment}") String deployment
    ) {
        this.restClient = RestClient.builder().baseUrl(endpoint + "/openai/v1").defaultHeader("api-key", apiKey).build();
        this.jsonMapper = jsonMapper;
        this.deployment = deployment;
    }

    public CandidateProfile extract(String cvText) {
        Map<String, Object> body = Map.of(
                "model", deployment,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", "CV:\n" + cvText)));

        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("No response from the AI");
        }

        String content = response.choices().get(0).message().content();

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
            throw new IllegalStateException("Could not read the AI model's answer: " + content, e);
        }
    }
    
    // Just the parts of the reply we need. Everything else in the JASON is ignored.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }
    
    @JsonIgnoreProperties (ignoreUnknown = true)
    record Choice(Message message) {
    }
    
    @JsonIgnoreProperties (ignoreUnknown = true)
    record Message(String content) {

    }
    
}
