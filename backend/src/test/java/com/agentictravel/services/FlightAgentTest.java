package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class FlightAgentTest {
    @Test
    public void flightAgentUsesLLM() throws ExecutionException, InterruptedException {
        FakeLLMClient fake = new FakeLLMClient("LLM response: cheap flight available");
        FlightAgent agent = new FlightAgent(fake);

        TripRequest req = new TripRequest();
        req.tripTitle = "Test Trip";
        req.days = 3;
        req.region = "TestLand";
        req.people = 2;

        Map<String,Object> res = agent.search(req).get();
        assertNotNull(res.get("recommended"));
        Map recommended = (Map)res.get("recommended");
        assertTrue(((String)recommended.get("notes")).contains("LLM response"));
    }
}
