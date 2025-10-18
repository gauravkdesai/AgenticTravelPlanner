package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class AgentCoordinatorTest {
    @Test
    public void coordinatorAggregatesAgents() throws ExecutionException, InterruptedException {
        FakeLLMClient fake = new FakeLLMClient("OK");
        AgentCoordinator coordinator = new AgentCoordinator(fake);

        TripRequest req = new TripRequest();
        req.tripTitle = "Coord Test";
        req.days = 1;
        req.region = "Nowhere";
        req.people = 1;

        var it = coordinator.generateItinerary(req).get();
        assertNotNull(it);
        assertNotNull(it.bookings);
    }
}
