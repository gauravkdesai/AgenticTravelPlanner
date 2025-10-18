package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherAgentTest {
    @Test
    public void weatherAgentUsesLLM() throws Exception {
        FakeLLMClient fake = new FakeLLMClient("Sunny with light breeze");
        WeatherAgent agent = new WeatherAgent(fake);

        TripRequest req = new TripRequest();
        req.region = "Beach";

        Map<String,Object> res = agent.search(req).get();
        assertTrue(((String)res.get("forecastSummary")).contains("Sunny"));
    }
}
