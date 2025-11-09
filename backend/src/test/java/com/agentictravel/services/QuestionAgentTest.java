package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class QuestionAgentTest {
    
    private QuestionAgent questionAgent;
    private FakeLLMClient fakeLLMClient;
    
    @BeforeEach
    void setUp() {
        fakeLLMClient = new FakeLLMClient("""
            {
                "questions": [
                    {
                        "question": "What specific cities interest you most?",
                        "type": "destination",
                        "required": true
                    },
                    {
                        "question": "What pace do you prefer?",
                        "type": "pace",
                        "options": ["Relaxed", "Moderate", "Fast-paced"],
                        "required": true
                    }
                ],
                "context": "Questions to help refine your travel preferences"
            }
            """);
        questionAgent = new QuestionAgent(fakeLLMClient);
    }
    
    @Test
    void testGenerateQuestions_Success() throws ExecutionException, InterruptedException {
        TripRequest request = createTestRequest();
        
        var result = questionAgent.generateQuestions(request).get();
        
        assertNotNull(result);
        assertNotNull(result.questions);
        assertEquals(2, result.questions.size());
        assertEquals("Questions to help refine your travel preferences", result.context);
        
        var firstQuestion = result.questions.get(0);
        assertEquals("What specific cities interest you most?", firstQuestion.question);
        assertEquals("destination", firstQuestion.type);
        assertTrue(firstQuestion.required);
        
        var secondQuestion = result.questions.get(1);
        assertEquals("What pace do you prefer?", secondQuestion.question);
        assertEquals("pace", secondQuestion.type);
        assertTrue(secondQuestion.required);
        assertNotNull(secondQuestion.options);
        assertEquals(3, secondQuestion.options.length);
    }
    
    @Test
    void testGenerateQuestions_NullRequest() throws ExecutionException, InterruptedException {
        var result = questionAgent.generateQuestions(new TripRequest()).get();
        
        assertNotNull(result);
        assertNotNull(result.questions);
        assertTrue(result.questions.size() > 0); // Should have fallback questions
    }
    
    @Test
    void testGenerateQuestions_EmptyRequest() throws ExecutionException, InterruptedException {
        TripRequest request = new TripRequest();
        
        var result = questionAgent.generateQuestions(request).get();
        
        assertNotNull(result);
        assertNotNull(result.questions);
    }
    
    @Test
    void testGenerateQuestions_InvalidJsonResponse() throws ExecutionException, InterruptedException {
        FakeLLMClient invalidClient = new FakeLLMClient("Invalid JSON response");
        QuestionAgent invalidAgent = new QuestionAgent(invalidClient);
        
        TripRequest request = createTestRequest();
        
        var result = invalidAgent.generateQuestions(request).get();
        
        assertNotNull(result);
        assertNotNull(result.questions);
        assertTrue(result.questions.size() > 0); // Should have fallback questions
    }
    
    @Test
    void testGenerateQuestions_WithSpecialCharacters() throws ExecutionException, InterruptedException {
        TripRequest request = createTestRequest();
        request.region = "Paris, France 🇫🇷";
        request.notes = "I want to visit cafés and museums";
        
        var result = questionAgent.generateQuestions(request).get();
        
        assertNotNull(result);
        assertNotNull(result.questions);
    }
    
    @Test
    void testGenerateQuestions_WithLongInput() throws ExecutionException, InterruptedException {
        TripRequest request = createTestRequest();
        request.notes = "a".repeat(1000);
        
        var result = questionAgent.generateQuestions(request).get();
        
        assertNotNull(result);
        assertNotNull(result.questions);
    }
    
    private TripRequest createTestRequest() {
        TripRequest request = new TripRequest();
        request.tripTitle = "Test Trip";
        request.days = 3;
        request.region = "Europe";
        request.people = 2;
        request.interests = java.util.List.of("culture", "food");
        request.notes = "Test notes";
        return request;
    }
}