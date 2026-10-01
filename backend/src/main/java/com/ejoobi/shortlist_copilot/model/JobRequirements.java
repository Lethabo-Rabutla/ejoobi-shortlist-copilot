package com.ejoobi.shortlist_copilot.model;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobRequirements(
        @NotBlank 
        String title,
        List<String> requiredSkills,
        int minYearExperience,
        String city,
        @NotNull 
        QualificationLevel minQualificationLevel) {
}
