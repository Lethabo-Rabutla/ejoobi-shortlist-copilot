package com.ejoobi.shortlist_copilot.service;

import com.ejoobi.shortlist_copilot.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceCandidatesTest {

    private final ScoringService scoring = new ScoringService();

    private final JobRequirements job = new JobRequirements(
            "Senior Python Developer",
            List.of("Python", "Java", "SQL"),
            3,
            "Johannesburg",
            QualificationLevel.DEGREE);

    private double total(CandidateProfile c) {
        return scoring.score(job, c).total();
    }

    @Test
    void strongCandidateScoresOneHundred() {
        CandidateProfile nomsa = new CandidateProfile("Nomsa", List.of("Python", "Java", "SQL"),
                6, "Sandton, Johannesburg", false, QualificationLevel.DEGREE);
        assertEquals(100.0, total(nomsa), 0.01);
    }

    @Test
    void mediumCandidateWhoWillRelocate() {
        CandidateProfile pieter = new CandidateProfile("Pieter", List.of("Python", "SQL"),
                4, "Cape Town", true, QualificationLevel.DEGREE);
        assertEquals(78.7, total(pieter), 0.01);
    }

    @Test
    void weakCandidateScoresLow() {
        CandidateProfile sipho = new CandidateProfile("Sipho", List.of("SQL"),
                1, "Durban", false, QualificationLevel.CERTIFICATE);
        assertEquals(26.6, total(sipho), 0.01);
    }

    @Test
    void fullSkillsCanOutweighAnExperienceGap() {
        // Known limitation, documented in docs/solution.md
        CandidateProfile ayesha = new CandidateProfile("Ayesha", List.of("Python", "Java", "SQL"),
                2, "Midrand", false, QualificationLevel.DIPLOMA);
        assertEquals(86.7, total(ayesha), 0.01);
    }

    @Test
    void randburgCountsAsJohannesburg() {
        CandidateProfile local = new CandidateProfile("Local", List.of("Python"),
                3, "Randburg Gauteng", false, QualificationLevel.DEGREE);
        assertEquals(20.0, scoring.score(job, local).location(), 0.01);
    }

    @Test
    void skillMatchingIgnoresCaseAndSpaces() {
        CandidateProfile c = new CandidateProfile("C", List.of(" python ", "JAVA", "sql"),
                3, "Johannesburg", false, QualificationLevel.DEGREE);
        assertEquals(40.0, scoring.score(job, c).skills(), 0.01);
    }
}
