package com.ejoobi.shortlist_copilot.controller;

import com.ejoobi.shortlist_copilot.model.*;
import com.ejoobi.shortlist_copilot.service.CvExtractionService;
import com.ejoobi.shortlist_copilot.service.ExplanationService;
import com.ejoobi.shortlist_copilot.service.ScoringService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EvaluationController {

    private final CvExtractionService extractionService;
    private final ScoringService scoringService;
    private final ExplanationService explanationService;

    public EvaluationController(CvExtractionService extractionService,
                                ScoringService scoringService,
                                ExplanationService explanationService) {
        this.extractionService = extractionService;
        this.scoringService = scoringService;
        this.explanationService = explanationService;
    }

    @PostMapping("/evaluate")
    public EvaluationResponse evaluate(@Valid @RequestBody EvaluationRequest request) {
        CandidateProfile candidate = extractionService.extract(request.cvText());
        ScoreBreakdown score = scoringService.score(request.job(), candidate);
        Explanation explanation = explanationService.explain(request.job(), candidate, score);
        return new EvaluationResponse(candidate, score, explanation);
    }
}