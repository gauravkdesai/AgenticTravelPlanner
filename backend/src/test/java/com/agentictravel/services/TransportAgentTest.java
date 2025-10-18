package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TransportAgentTest {
    @Test
    public void transportAgentUsesLLM() throws Exception {
        FakeLLMClient fake = new FakeLLMClient("Trains and cars available");
        TransportAgent agent = new TransportAgent(fake);

        TripRequest req = new TripRequest();
        req.region = "TestLand";
        req.people = 2;

        Map<String,Object> res = agent.search(req).get();
        assertNotNull(res.get("carRental"));
        assertNotNull(res.get("trainOptions"));
    }
}
