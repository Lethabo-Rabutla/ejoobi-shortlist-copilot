package com.ejoobi.shortlist_copilot.model;
import java.util.List;

public record CandidateProfile(
        String name,
        List<String> skills,
        double yearsExperience,
        String city,
        boolean willingToRelocate,
        QualificationLevel highestQualificationLevel) {
}
