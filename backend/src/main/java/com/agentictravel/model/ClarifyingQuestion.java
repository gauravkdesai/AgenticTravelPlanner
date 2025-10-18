package com.agentictravel.model;

public class ClarifyingQuestion {
    public String question;
    public String type; // "destination", "activity", "pace", "budget", "preference"
    public String[] options; // Optional predefined options
    public boolean required;
    
    public ClarifyingQuestion() {}
    
    public ClarifyingQuestion(String question, String type, boolean required) {
        this.question = question;
        this.type = type;
        this.required = required;
    }
    
    public ClarifyingQuestion(String question, String type, String[] options, boolean required) {
        this.question = question;
        this.type = type;
        this.options = options;
        this.required = required;
    }
}
