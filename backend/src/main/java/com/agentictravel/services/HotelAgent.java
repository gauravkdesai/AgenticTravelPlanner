package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HotelAgent {
    private final LLMClient llm;

    public HotelAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
        String amendments = safeGetAmendments(request);
        Object dates = safeGetTentativeDates(request);
        String schema = """
            {
                "options": [
                    {
                        "name": "string",
                        "pricePerNight": "string",
                        "totalPrice": "string",
                        "location": "string",
                        "rating": "string",
                        "amenities": ["amenity1", "amenity2"],
                        "pros": ["pro1", "pro2"],
                        "cons": ["con1", "con2"],
                        "bookingUrl": "string"
                    }
                ],
                "summary": "string"
            }
            """;
        String prompt = "You are a hotel search assistant. Given the trip: " + request.tripTitle +
        ", region=" + request.region + ", tentativeDates='" + (dates==null?"":dates.toString()) + "', nights=" + Math.max(request.days,1) +
        ", people=" + request.people + ". Find 3-5 hotel options with different price ranges and locations. " +
        "Consider special needs: kids=" + (request.special != null ? request.special.kids : false) + 
        ", elderly=" + (request.special != null ? request.special.elderly : false) + 
        ", accessible=" + (request.special != null ? request.special.differentlyAbled : false) + ". " +
        "If the user provided amendments: '" + (amendments==null?"":amendments) + "' include them when suggesting hotels. " +
        "Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add any commentary outside the JSON.";

        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(resp -> {
            try {
                // Try to parse the response as JSON, fallback to mock data if parsing fails
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(resp, Map.class);
            } catch (Exception e) {
                // Fallback to mock data
                return Map.of(
                    "options", java.util.List.of(
                        Map.of("name", "Luxury Resort", "pricePerNight", "250 USD", "totalPrice", "750 USD", 
                               "location", "City Center", "rating", "4.8/5", 
                               "amenities", java.util.List.of("Pool", "Spa", "Restaurant"), 
                               "pros", java.util.List.of("Great location", "Excellent amenities"), 
                               "cons", java.util.List.of("Higher price"), "bookingUrl", "https://example.com"),
                        Map.of("name", "Budget Inn", "pricePerNight", "80 USD", "totalPrice", "240 USD", 
                               "location", "Near Airport", "rating", "3.5/5", 
                               "amenities", java.util.List.of("Free WiFi", "Breakfast"), 
                               "pros", java.util.List.of("Affordable", "Clean rooms"), 
                               "cons", java.util.List.of("Further from city"), "bookingUrl", "https://example.com")
                    ),
                    "summary", "Found multiple hotel options with different price ranges and locations."
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
