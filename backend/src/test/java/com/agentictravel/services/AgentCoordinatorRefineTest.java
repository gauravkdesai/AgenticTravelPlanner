package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class AgentCoordinatorRefineTest {
    @Test
    public void refineFlowParsesLLMJsonNotes() throws ExecutionException, InterruptedException {
        // Return JSON in responses so parser picks it up
        String flightJson = "{\"recommended\":{\"carrier\":\"DemoAir\",\"price\":\"400 USD\",\"notes\":\"{\\\"seat\\\":\\\"aisle\\\"}\"},\"alternatives\":[] }";
        FakeLLMClient fake = new FakeLLMClient(flightJson);
        AgentCoordinator coordinator = new AgentCoordinator(fake);

        TripRequest req = new TripRequest();
        req.tripTitle = "Refine Test";
        req.days = 2;
        req.region = "Testland";
        req.people = 1;
        req.amendments = "Make it 3 days";

        var it = coordinator.generateItinerary(req).get();
        assertNotNull(it);
        assertNotNull(it.bookings);
        // If parser ran, flights map should contain notes_parsed
        Object flights = it.bookings.flights;
        assertTrue(flights instanceof java.util.Map);
        java.util.Map<?,?> flightsMap = (java.util.Map<?,?>) flights;
        Object recommended = flightsMap.get("recommended");
        assertTrue(recommended instanceof java.util.Map);
        java.util.Map<?,?> rec = (java.util.Map<?,?>) recommended;
        assertTrue(rec.containsKey("notes"));
        // notes_parsed should be present when JSON parsed
        assertTrue(rec.containsKey("notes_parsed") || flightsMap.containsKey("notes_parsed"));
    }
}
