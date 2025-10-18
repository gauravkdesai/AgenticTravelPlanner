package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EventAgent {
    private final LLMClient llm;

    public EventAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<List<Map<String,Object>>> search(TripRequest request){
        String amendments = safeGetAmendments(request);
        Object dates = safeGetTentativeDates(request);
        String schema = """
            {
                "events": [
                    {
                        "name": "string",
                        "date": "string",
                        "time": "string",
                        "location": "string",
                        "description": "string",
                        "category": "string",
                        "price": "string",
                        "duration": "string",
                        "bookingUrl": "string"
                    }
                ],
                "summary": "string"
            }
            """;
        String prompt = "You are an events and activities assistant. Given region " + request.region +
        ", tentative dates '" + (dates==null?"":dates.toString()) + "', interests: " + (request.interests!=null?request.interests.toString():"general") +
        ", and user amendments '" + (amendments==null?"":amendments) + "'. " +
        "Find 5-10 relevant events, activities, attractions, or experiences that would be suitable for this trip. " +
        "Consider special needs: kids=" + (request.special != null ? request.special.kids : false) + 
        ", elderly=" + (request.special != null ? request.special.elderly : false) + 
        ", accessible=" + (request.special != null ? request.special.differentlyAbled : false) + ". " +
        "Return ONLY a valid JSON object matching this schema: \n" + schema + "\nDo not add any commentary outside the JSON.";

        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(resp -> {
            try {
                // Try to parse the response as JSON, fallback to mock data if parsing fails
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> responseMap = mapper.readValue(resp, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> events = (List<Map<String, Object>>) responseMap.get("events");
                return events != null ? events : java.util.List.of();
            } catch (Exception e) {
                // Fallback to mock data
                return java.util.List.of(
                    Map.of("name", "City Museum Tour", "date", "2025-01-15", "time", "10:00", 
                           "location", "City Center", "description", "Guided tour of local history", 
                           "category", "Culture", "price", "15 USD", "duration", "2h", "bookingUrl", "https://example.com"),
                    Map.of("name", "Food Market Visit", "date", "2025-01-16", "time", "14:00", 
                           "location", "Old Town", "description", "Local food tasting experience", 
                           "category", "Food", "price", "25 USD", "duration", "3h", "bookingUrl", "https://example.com"),
                    Map.of("name", "Scenic Walking Tour", "date", "2025-01-17", "time", "09:00", 
                           "location", "Historic District", "description", "Explore historic landmarks", 
                           "category", "Sightseeing", "price", "Free", "duration", "2h", "bookingUrl", "https://example.com")
                );
            }
        });
    }

    private Object safeGetTentativeDates(TripRequest request){
        try{
            java.lang.reflect.Field f = request.getClass().getField("tentativeDates");
            return f.get(request);
        }catch(Exception e){ return null; }
    }

    private String safeGetAmendments(TripRequest request){
        try {
            java.lang.reflect.Method m = request.getClass().getMethod("getAmendments");
            Object val = m.invoke(request);
            return val==null?null:val.toString();
        } catch (Exception e){
            try {
                java.lang.reflect.Field f = request.getClass().getField("amendments");
                Object val = f.get(request);
                return val==null?null:val.toString();
            } catch (Exception ex){
                return null;
            }
        }
    }
}
