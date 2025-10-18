package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class HotelAgentTest {
    @Test
    public void hotelAgentUsesLLM() throws ExecutionException, InterruptedException {
        FakeLLMClient fake = new FakeLLMClient("LLM says: 3 hotels available");
        HotelAgent agent = new HotelAgent(fake);

        TripRequest req = new TripRequest();
        req.tripTitle = "Test Trip";
        req.days = 2;
        req.region = "TestVille";

        Map<String,Object> res = agent.search(req).get();
        assertNotNull(res.get("recommended"));
        Map recommended = (Map)res.get("recommended");
        assertTrue(((String)recommended.get("notes")).contains("LLM says"));
    }
}
