package com.ejoobi.shortlist_copilot.controller;

import com.ejoobi.shortlist_copilot.model.*;
import com.ejoobi.shortlist_copilot.service.CvExtractionService;
import com.ejoobi.shortlist_copilot.service.ScoringService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EvaluationController {

    private final CvExtractionService extractionService;
    private final ScoringService scoringService;

    public EvaluationController(CvExtractionService extractionService,
                                ScoringService scoringService) {
        this.extractionService = extractionService;
        this.scoringService = scoringService;
    }

    @PostMapping("/evaluate")
    public EvaluationResponse evaluate(@Valid @RequestBody EvaluationRequest request) {
        CandidateProfile candidate = extractionService.extract(request.cvText());
        ScoreBreakdown score = scoringService.score(request.job(), candidate);
        return new EvaluationResponse(candidate, score);
    }
}