package com.agentictravel.services;

import com.agentictravel.llm.FakeLLMClient;
import com.agentictravel.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EventAgentTest {
    @Test
    public void eventAgentUsesLLM() throws Exception {
        FakeLLMClient fake = new FakeLLMClient("Events found");
        EventAgent agent = new EventAgent(fake);

        TripRequest req = new TripRequest();
        req.region = "City";

        List<Map<String,Object>> res = agent.search(req).get();
        assertFalse(res.isEmpty());
    }
}
