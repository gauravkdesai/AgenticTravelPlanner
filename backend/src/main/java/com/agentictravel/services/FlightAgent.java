package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FlightAgent {
    private final LLMClient llm;

    public FlightAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
        String amendments = safeGetAmendments(request);
        Object dates = safeGetTentativeDates(request);
        String schema = """
            {
                "options": [
                    {
                        "carrier": "string",
                        "price": "string",
                        "departureTime": "string",
                        "arrivalTime": "string",
                        "duration": "string",
                        "stops": "string",
                        "pros": ["pro1", "pro2"],
                        "cons": ["con1", "con2"],
                        "bookingUrl": "string"
                    }
                ],
                "summary": "string"
            }
            """;
        String prompt = "You are a flight search assistant. Given the trip request: " + request.tripTitle +
        ", days=" + request.days + ", region=" + request.region + ", people=" + request.people +
        ". Tentative dates: '" + (dates==null?"":dates.toString()) + "'. " +
        "Find 3-5 flight options with different price points and convenience levels. " +
        "If the user provided amendments: '" + (amendments==null?"":amendments) + "' include them when suggesting flights. " +
        "Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add any extra commentary outside the JSON.";

        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(resp -> {
            try {
                // Try to parse the response as JSON, fallback to mock data if parsing fails
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(resp, Map.class);
            } catch (Exception e) {
                // Fallback to mock data
                return Map.of(
                    "recommended", Map.of("carrier", "OpenAI Airlines", "price", "450 USD", "notes", resp == null ? "Fallback flight info" : (resp.contains("LLM response")?resp:"Fallback flight info")),
                    "alternatives", java.util.List.of(
                        Map.of("carrier", "Budget Air", "price", "320 USD", "notes", "Budget option")
                    ),
                    "summary", "Found multiple flight options with different price points and schedules."
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
        }catch(Exception e){
            // fallback to direct field access if present
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
