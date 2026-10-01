package com.ejoobi.shortlist_copilot.model;
import java.util.List;

public record JobRequirements(
        String title,
        List<String> requiredSkills,
        int minYearExperience,
        String city,
        QualificationLevel minQualificationLevel) {
}
