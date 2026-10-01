package com.ejoobi.shortlist_copilot.service;

import com.ejoobi.shortlist_copilot.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoringServiceTest {
    private final ScoringService scoringService = new ScoringService();

    @Test 
    void thaboScoresFifty() {
        JobRequirements job = new JobRequirements(
                "Senior Python Developer",
                List.of("Python", "Java", "SQL"),
                3,
                "Johannesburg",
                QualificationLevel.DEGREE);

        CandidateProfile thabo = new CandidateProfile(
                "Thabo",
                List.of("Python", "SQL"),
                1,
                "Durban",
                false,
                QualificationLevel.DEGREE);
        ScoreBreakdown result = scoringService.score(job, thabo);

        assertEquals(26.7, result.skills(), 0.01);
        assertEquals(8.3, result.experience(), 0.01);
        assertEquals(0.0, result.location(), 0.01);
        assertEquals(15.0, result.qualifications(), 0.01);
        assertEquals(50.0, result.total(), 0.01);
    }
    
}
