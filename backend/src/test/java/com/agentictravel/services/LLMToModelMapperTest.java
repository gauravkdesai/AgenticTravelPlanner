package com.agentictravel.services;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LLMToModelMapperTest {
    @Test
    public void missingRecommendedDoesNotExplode(){
        Map<String,Object> flights = Map.of();
        Map<String,Object> transport = Map.of();
        Map<String,Object> hotels = Map.of();
        var b = LLMToModelMapper.mapToBooking(flights, transport, hotels);
        assertNotNull(b);
        assertNull(b.flightsTyped);
        assertNull(b.transportTyped);
        assertNull(b.hotelsTyped);
    }

    @Test
    public void unparsableNotesHandledGracefully(){
        Map<String,Object> flights = Map.of("recommended", Map.of("carrier","X","price","100","notes","{ not valid json"));
        var b = LLMToModelMapper.mapToBooking(flights, Map.of(), Map.of());
        // notes_parsed won't exist; typed flight may still be present but notes null
        if(b.flightsTyped!=null){
            assertNull(b.flightsTyped.notes);
        }
    }

    @Test
    public void malformedDayPlansIgnored(){
        Object malformed = "this is not a list";
        var days = LLMToModelMapper.mapToDayPlans(malformed);
        assertNotNull(days);
        assertEquals(0, days.size());
    }
}
