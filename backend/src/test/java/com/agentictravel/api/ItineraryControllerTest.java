package com.agentictravel.api;

import com.agentictravel.model.Itinerary;
import com.agentictravel.model.TripRequest;
import com.agentictravel.services.AgentCoordinator;
import com.agentictravel.validation.TripRequestValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class ItineraryControllerTest {
    @Test
    public void controllerReturnsItinerary() throws Exception {
        AgentCoordinator coord = new AgentCoordinator(new com.agentictravel.llm.FakeLLMClient("ok")){
            @Override
            public CompletableFuture<Itinerary> generateItinerary(TripRequest request){
                Itinerary sample = new Itinerary();
                sample.summary = "ok";
                return CompletableFuture.completedFuture(sample);
            }
        };
        TripRequestValidator validator = Mockito.mock(TripRequestValidator.class);
        Mockito.when(validator.validate(Mockito.any())).thenReturn(
            new TripRequestValidator.ValidationResult(new ArrayList<>(), new ArrayList<>()));
        ItineraryController ctrl = new ItineraryController(coord, validator);
        TripRequest req = new TripRequest();
        var fut = ctrl.createItinerary(req);
        var resp = fut.get();
        assertEquals("ok", resp.getBody().summary);
    }
}