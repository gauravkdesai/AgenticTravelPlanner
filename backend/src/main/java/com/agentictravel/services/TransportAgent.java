package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TransportAgent {
    private final LLMClient llm;

    public TransportAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
        String amendments = safeGetAmendments(request);
        Object dates = safeGetTentativeDates(request);
        String schema = """
            {
                "carRental": [
                    {
                        "provider": "string",
                        "pricePerDay": "string",
                        "totalPrice": "string",
                        "carType": "string",
                        "pros": ["pro1", "pro2"],
                        "cons": ["con1", "con2"],
                        "bookingUrl": "string"
                    }
                ],
                "trainOptions": [
                    {
                        "provider": "string",
                        "price": "string",
                        "duration": "string",
                        "route": "string",
                        "pros": ["pro1", "pro2"],
                        "cons": ["con1", "con2"],
                        "bookingUrl": "string"
                    }
                ],
                "busOptions": [
                    {
                        "provider": "string",
                        "price": "string",
                        "duration": "string",
                        "route": "string",
                        "pros": ["pro1", "pro2"],
                        "cons": ["con1", "con2"],
                        "bookingUrl": "string"
                    }
                ],
                "summary": "string"
            }
            """;
        String prompt = "You are a transport search assistant. Given trip to " + request.region +
        ", tentativeDates='" + (dates==null?"":dates.toString()) + "', for " + request.people + " people and preferences " + (request.bookingPreferences!=null?request.bookingPreferences.toString():"none") +
        ". Find multiple transport options including car rental, trains, and buses. " +
        "Consider special needs: kids=" + (request.special != null ? request.special.kids : false) + 
        ", elderly=" + (request.special != null ? request.special.elderly : false) + 
        ", accessible=" + (request.special != null ? request.special.differentlyAbled : false) + ". " +
        "If user amendments: '" + (amendments==null?"":amendments) + "' include them in consideration. " +
        "Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add any extra commentary.";

        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(resp -> {
            try {
                // Try to parse the response as JSON, fallback to mock data if parsing fails
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(resp, Map.class);
            } catch (Exception e) {
                // Fallback to mock data
                return Map.of(
                    "carRental", java.util.List.of(
                        Map.of("provider", "RentACar Pro", "pricePerDay", "45 USD", "totalPrice", "135 USD", 
                               "carType", "Compact", "pros", java.util.List.of("Flexible", "Door-to-door"), 
                               "cons", java.util.List.of("Parking costs"), "bookingUrl", "https://example.com")
                    ),
                    "trainOptions", java.util.List.of(
                        Map.of("provider", "Rail Express", "price", "25 USD", "duration", "2h 15m", 
                               "route", "City to City", "pros", java.util.List.of("Scenic route", "Comfortable"), 
                               "cons", java.util.List.of("Fixed schedule"), "bookingUrl", "https://example.com")
                    ),
                    "busOptions", java.util.List.of(
                        Map.of("provider", "Budget Bus", "price", "15 USD", "duration", "3h 30m", 
                               "route", "City to City", "pros", java.util.List.of("Cheapest option"), 
                               "cons", java.util.List.of("Longer journey"), "bookingUrl", "https://example.com")
                    ),
                    "summary", "Found multiple transport options with different price points and convenience levels."
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
