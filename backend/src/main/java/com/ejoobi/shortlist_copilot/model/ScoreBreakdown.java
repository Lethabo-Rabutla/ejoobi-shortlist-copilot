package com.ejoobi.shortlist_copilot.model;


public record ScoreBreakdown(
        double skills,           // out of 40
        double experience,      // out of 25
        double location,        // out of 20
        double qualifications,  // out of 15
        double total            // out of 100
){
    
}
