package com.agentictravel.api;

import com.agentictravel.model.Itinerary;
import com.agentictravel.model.TripRequest;
import com.agentictravel.model.QuestionResponse;
import com.agentictravel.services.AgentCoordinator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/itineraries")
@CrossOrigin(origins = "*")
public class ItineraryController {

    private final AgentCoordinator coordinator;

    public ItineraryController(AgentCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @PostMapping("/questions")
    public CompletableFuture<ResponseEntity<QuestionResponse>> generateQuestions(@RequestBody TripRequest request) {
        return coordinator.generateQuestions(request)
                .thenApply(questions -> ResponseEntity.ok(questions));
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Itinerary>> createItinerary(@RequestBody TripRequest request) {
        // Kick off coordinator which runs agents and returns an aggregated itinerary
        return coordinator.generateItinerary(request)
                .thenApply(it -> ResponseEntity.ok(it));
    }
}
