package com.agentictravel.api;

import com.agentictravel.model.Itinerary;
import com.agentictravel.model.TripRequest;
import com.agentictravel.model.QuestionResponse;
import com.agentictravel.services.AgentCoordinator;
import com.agentictravel.validation.TripRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/itineraries")
@CrossOrigin(origins = "${security.cors.allowed-origins:http://localhost:8080}")
public class ItineraryController {

    private static final Logger log = LoggerFactory.getLogger(ItineraryController.class);
    
    private final AgentCoordinator coordinator;
    private final TripRequestValidator validator;

    public ItineraryController(AgentCoordinator coordinator, TripRequestValidator validator) {
        this.coordinator = coordinator;
        this.validator = validator;
    }

    @PostMapping("/questions")
    public CompletableFuture<ResponseEntity<?>> generateQuestions(@Valid @RequestBody TripRequest request) {
        log.info("Generating questions for trip: {}", request.tripTitle);
        
        // Validate and sanitize the request
        var validationResult = validator.validate(request);
        if (!validationResult.isValid()) {
            log.warn("Invalid trip request: {}", validationResult.getFirstError());
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(validationResult.getErrors())
            );
        }
        
        if (validationResult.hasWarnings()) {
            log.info("Validation warnings: {}", validationResult.getWarnings());
        }
        
        return coordinator.generateQuestions(request)
                .thenApply(questions -> {
                    log.info("Generated {} questions for trip: {}", 
                        questions.questions.size(), request.tripTitle);
                    return ResponseEntity.ok(questions);
                })
                .exceptionally(throwable -> {
                    log.error("Failed to generate questions for trip: {}", request.tripTitle, throwable);
                    return ResponseEntity.internalServerError()
                        .body("Failed to generate questions: " + throwable.getMessage());
                });
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<?>> createItinerary(@Valid @RequestBody TripRequest request) {
        log.info("Creating itinerary for trip: {}", request.tripTitle);
        
        // Validate and sanitize the request
        var validationResult = validator.validate(request);
        if (!validationResult.isValid()) {
            log.warn("Invalid trip request: {}", validationResult.getFirstError());
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(validationResult.getErrors())
            );
        }
        
        if (validationResult.hasWarnings()) {
            log.info("Validation warnings: {}", validationResult.getWarnings());
        }
        
        return coordinator.generateItinerary(request)
                .thenApply(itinerary -> {
                    log.info("Successfully created itinerary for trip: {} with {} day plans", 
                        request.tripTitle, itinerary.dayPlans.size());
                    return ResponseEntity.ok(itinerary);
                })
                .exceptionally(throwable -> {
                    log.error("Failed to create itinerary for trip: {}", request.tripTitle, throwable);
                    return ResponseEntity.internalServerError()
                        .body("Failed to create itinerary: " + throwable.getMessage());
                });
    }
}
