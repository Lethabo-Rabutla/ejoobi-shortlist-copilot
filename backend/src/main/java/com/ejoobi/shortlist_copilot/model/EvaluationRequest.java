package com.ejoobi.shortlist_copilot.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationRequest(
    @NotBlank 
    String cvText,
    @NotNull
    @Valid JobRequirements job) {
}
