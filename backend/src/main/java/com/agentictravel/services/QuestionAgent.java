package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;
import com.agentictravel.model.ClarifyingQuestion;
import com.agentictravel.model.QuestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QuestionAgent {
    private final LLMClient llm;
    private final ObjectMapper objectMapper;
    
    public QuestionAgent(LLMClient llm) {
        this.llm = llm;
        this.objectMapper = new ObjectMapper();
    }
    
    public CompletableFuture<QuestionResponse> generateQuestions(TripRequest request) {
        String schema = """
            {
                "questions": [
                    {
                        "question": "string",
                        "type": "destination|activity|pace|budget|preference",
                        "options": ["option1", "option2"],
                        "required": boolean
                    }
                ],
                "context": "string"
            }
            """;
            
        String prompt = String.format("""
            You are a travel planning assistant. Analyze this trip request and generate 2-4 clarifying questions 
            that would help create a better itinerary. Focus on areas where the request is vague or could benefit 
            from more specificity.
            
            Trip Request:
            - Title: %s
            - Days: %d
            - Region: %s
            - Budget: %s
            - People: %d
            - Weather Preference: %s
            - Interests: %s
            - Special Needs: Kids=%b, Elderly=%b, Differently-abled=%b
            - Notes: %s
            
            Generate questions that help clarify:
            1. Specific destinations within the region
            2. Activity preferences and pace
            3. Budget priorities
            4. Must-see attractions or experiences
            
            Return ONLY valid JSON matching this schema:
            %s
            """, 
            request.tripTitle != null ? request.tripTitle : "Untitled Trip",
            request.days,
            request.region != null ? request.region : "Not specified",
            request.budget != null ? request.budget : "Not specified",
            request.people,
            request.weatherPreference != null ? request.weatherPreference : "Any",
            request.interests != null ? String.join(", ", request.interests) : "Not specified",
            request.special != null ? request.special.kids : false,
            request.special != null ? request.special.elderly : false,
            request.special != null ? request.special.differentlyAbled : false,
            request.notes != null ? request.notes : "None",
            schema
        );
        
        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(response -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(response);
                List<ClarifyingQuestion> questions = new ArrayList<>();
                
                JsonNode questionsNode = jsonNode.path("questions");
                if (questionsNode.isArray()) {
                    for (JsonNode questionNode : questionsNode) {
                        ClarifyingQuestion question = new ClarifyingQuestion();
                        question.question = questionNode.path("question").asText();
                        question.type = questionNode.path("type").asText();
                        question.required = questionNode.path("required").asBoolean(false);
                        
                        JsonNode optionsNode = questionNode.path("options");
                        if (optionsNode.isArray()) {
                            List<String> optionsList = new ArrayList<>();
                            for (JsonNode option : optionsNode) {
                                optionsList.add(option.asText());
                            }
                            question.options = optionsList.toArray(new String[0]);
                        }
                        
                        questions.add(question);
                    }
                }
                
                String context = jsonNode.path("context").asText("Questions to help refine your travel preferences.");
                
                return new QuestionResponse(questions, context);
                
            } catch (Exception e) {
                // Fallback to default questions if JSON parsing fails
                List<ClarifyingQuestion> defaultQuestions = new ArrayList<>();
                defaultQuestions.add(new ClarifyingQuestion(
                    "What specific cities or attractions are you most interested in visiting?", 
                    "destination", true));
                defaultQuestions.add(new ClarifyingQuestion(
                    "What pace do you prefer for your trip?", 
                    "pace", 
                    new String[]{"Relaxed", "Moderate", "Fast-paced"}, 
                    true));
                
                return new QuestionResponse(defaultQuestions, 
                    "Default questions to help refine your travel preferences.");
            }
        });
    }
}
