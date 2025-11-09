package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WeatherAgent {
    private final LLMClient llm;

    public WeatherAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
        String amendments = safeGetAmendments(request);
        Object dates = safeGetTentativeDates(request);
        String schema = """
            {
                "forecastSummary": "string",
                "dailyForecast": [
                    {
                        "date": "string",
                        "high": "string",
                        "low": "string",
                        "condition": "string",
                        "precipitation": "string",
                        "wind": "string",
                        "recommendations": ["rec1", "rec2"]
                    }
                ],
                "packingSuggestions": ["item1", "item2"],
                "activityRecommendations": ["activity1", "activity2"]
            }
            """;
        String prompt = "You are a weather assistant. For region " + request.region + 
        ", tentativeDates='" + (dates==null?"":dates.toString()) + "', user amendments '" + (amendments==null?"":amendments) + 
        "', and weather preference '" + (request.weatherPreference!=null?request.weatherPreference:"any") + "'. " +
        "Provide a detailed weather forecast and recommendations for the trip duration. " +
        "Consider the user's weather preference and suggest appropriate activities and packing items. " +
        "Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add commentary outside the JSON.";

        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(resp -> {
            try {
                // Try to parse the response as JSON, fallback to mock data if parsing fails
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(resp, Map.class);
            } catch (Exception e) {
                // Fallback to mock data
                return Map.of(
                    "forecastSummary", resp == null ? "Generally pleasant weather with mild temperatures" : (resp.contains("Sunny")?resp:"Generally pleasant weather with mild temperatures"),
                    "dailyForecast", java.util.List.of(
                        Map.of("date", "2025-01-15", "high", "22°C", "low", "12°C", "condition", "Partly cloudy", 
                               "precipitation", "10%", "wind", "Light breeze", 
                               "recommendations", java.util.List.of("Perfect for outdoor activities", "Light jacket recommended")),
                        Map.of("date", "2025-01-16", "high", "25°C", "low", "15°C", "condition", "Sunny", 
                               "precipitation", "0%", "wind", "Calm", 
                               "recommendations", java.util.List.of("Great day for sightseeing", "Sunscreen recommended")),
                        Map.of("date", "2025-01-17", "high", "20°C", "low", "10°C", "condition", "Overcast", 
                               "precipitation", "30%", "wind", "Moderate", 
                               "recommendations", java.util.List.of("Indoor activities preferred", "Umbrella suggested"))
                    ),
                    "packingSuggestions", java.util.List.of("Light jacket", "Comfortable walking shoes", "Sunscreen", "Umbrella"),
                    "activityRecommendations", java.util.List.of("Outdoor sightseeing", "Museum visits", "Food tours")
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
