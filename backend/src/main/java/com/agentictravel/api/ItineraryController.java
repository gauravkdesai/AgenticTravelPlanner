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

    private static final Logger LOG = LoggerFactory.getLogger(ItineraryController.class);
    
    private final AgentCoordinator coordinator;
    private final TripRequestValidator validator;

    public ItineraryController(AgentCoordinator coordinator, TripRequestValidator validator) {
        this.coordinator = coordinator;
        this.validator = validator;
    }

    @PostMapping("/questions")
    public CompletableFuture<ResponseEntity<QuestionResponse>> generateQuestions(@Valid @RequestBody TripRequest request) {
        LOG.info("Generating questions for trip: {}", request.tripTitle);
        
        // Validate and sanitize the request
        var validationResult = validator.validate(request);
        if (!validationResult.isValid()) {
            LOG.warn("Invalid trip request: {}", validationResult.getFirstError());
            return CompletableFuture.completedFuture(
                ResponseEntity.<QuestionResponse>badRequest().body(null)
            );
        }
        
        if (validationResult.hasWarnings()) {
            LOG.info("Validation warnings found.");
        }
        
        return coordinator.generateQuestions(request)
                .thenApply(questions -> {
                    LOG.info("Generated questions for trip: {}", request.tripTitle);
                    return ResponseEntity.ok(questions);
                })
                .exceptionally(throwable -> {
                    LOG.error("Failed to generate questions for trip: {}", request.tripTitle, throwable);
                    return ResponseEntity.<QuestionResponse>internalServerError()
                        .body(null);
                });
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Itinerary>> createItinerary(@Valid @RequestBody TripRequest request) {
        LOG.info("Creating itinerary for trip: {}", request.tripTitle);
        
        // Validate and sanitize the request
        var validationResult = validator.validate(request);
        if (!validationResult.isValid()) {
            LOG.warn("Invalid trip request: {}", validationResult.getFirstError());
            return CompletableFuture.completedFuture(
                ResponseEntity.<Itinerary>badRequest().body(null)
            );
        }
        
        if (validationResult.hasWarnings()) {
            LOG.info("Validation warnings found.");
        }
        
        return coordinator.generateItinerary(request)
                .thenApply(itinerary -> {
                    LOG.info("Successfully created itinerary for trip: {}", 
                        request.tripTitle);
                    return ResponseEntity.ok(itinerary);
                })
                .exceptionally(throwable -> {
                    LOG.error("Failed to create itinerary for trip: {}", request.tripTitle, throwable);
                    return ResponseEntity.<Itinerary>internalServerError()
                        .body(null);
                });
    }
}