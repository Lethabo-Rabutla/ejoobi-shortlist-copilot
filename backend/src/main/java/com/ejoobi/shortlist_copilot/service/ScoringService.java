package com.ejoobi.shortlist_copilot.service;

import com.ejoobi.shortlist_copilot.model.*;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private static final double SKILLS_MAX = 40;
    private static final double EXPERIENCE_MAX = 25;
    private static final double LOCATION_MAX = 20;
    private static final double QUALIFICATIONS_MAX = 15;

    public ScoreBreakdown score(JobRequirements job, CandidateProfile candidate) {
        double skills = round(SKILLS_MAX * skillsRatio(job, candidate));
        double experience = round(EXPERIENCE_MAX * experienceRatio(job, candidate));
        double location = round(LOCATION_MAX * locationRatio(job, candidate));
        double qualifications = round(QUALIFICATIONS_MAX * qualificationRatio(job, candidate));
        double total = round(skills + experience + location + qualifications);
        return new ScoreBreakdown(skills, experience, location, qualifications, total);
    }

    // What percentage of the required skills does the candidate have?
    private double skillsRatio(JobRequirements job, CandidateProfile candidate) {

        if (job.requiredSkills() == null || job.requiredSkills().isEmpty())
            return 1.0;
        Set<String> have = candidate.skills() == null ? Set.of()
                : candidate.skills().stream().map(s -> s.trim().toLowerCase()).collect(Collectors.toSet());
        long matched = job.requiredSkills().stream().map(s -> s.trim().toLowerCase()).filter(have::contains).count();

        return (double) matched / job.requiredSkills().size();
    }
    
    // Candidate's years divided by required years, capped at 100%
    private double experienceRatio(JobRequirements job, CandidateProfile candidate) {
        if (job.minYearExperience() <= 0)
            return 1.0;
        return Math.min(1.0, candidate.yearsExperience() / job.minYearExperience());
    }

    // Same city = full marks. Willing to relocate = 60%. Otherwise 0%.
    private double locationRatio(JobRequirements job, CandidateProfile candidate) {
        if (job.city() != null && job.city().equalsIgnoreCase(candidate.city()))
            return 1.0;
        return candidate.willingToRelocate() ? 0.6 : 0.0;
    }

    // Meets or exceeds the required level = full marks, otherwise partial credit.
    private double qualificationRatio(JobRequirements job, CandidateProfile candidate) {
        int required = job.minQualificationLevel().ordinal();
        int actual = candidate.highestQualificationLevel().ordinal();

        return (double) actual / required;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
