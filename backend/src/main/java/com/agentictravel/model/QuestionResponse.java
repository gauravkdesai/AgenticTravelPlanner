package com.agentictravel.model;

import java.util.List;

public class QuestionResponse {
    public List<ClarifyingQuestion> questions;
    public String context; // Additional context about why these questions are being asked
    
    public QuestionResponse() {}
    
    public QuestionResponse(List<ClarifyingQuestion> questions, String context) {
        this.questions = questions;
        this.context = context;
    }
}
