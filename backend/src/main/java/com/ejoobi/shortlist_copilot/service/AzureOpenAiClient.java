package com.ejoobi.shortlist_copilot.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AzureOpenAiClient {

    private final RestClient restClient;
    private final String deployment;

    public AzureOpenAiClient(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.deployment}") String deployment) {
        this.restClient = RestClient.builder()
                .baseUrl(endpoint + "/openai/v1")
                .defaultHeader("api-key", apiKey)
                .build();
        this.deployment = deployment;
    }

    /** Sends a system prompt and a user message, returns the model's JSON reply as text. */
    public String chatJson(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", deployment,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)));

        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("The AI model returned an empty response");
        }
        return response.choices().get(0).message().content();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}
}