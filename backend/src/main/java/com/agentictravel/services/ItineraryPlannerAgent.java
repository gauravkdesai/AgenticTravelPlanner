package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;
import com.agentictravel.model.DayPlan;
import com.agentictravel.model.Activity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ItineraryPlannerAgent {
    private final LLMClient llm;
    private final ObjectMapper objectMapper;
    
    public ItineraryPlannerAgent(LLMClient llm) {
        this.llm = llm;
        this.objectMapper = new ObjectMapper();
    }
    
    public CompletableFuture<List<DayPlan>> createDayPlans(
            TripRequest request,
            Map<String, Object> flights,
            Map<String, Object> hotels,
            Map<String, Object> transport,
            List<Map<String, Object>> events,
            Map<String, Object> weather) {
        
        String schema = """
            {
                "dayPlans": [
                    {
                        "dayNumber": 1,
                        "title": "string",
                        "activities": [
                            {
                                "title": "string",
                                "time": "string",
                                "duration": "string",
                                "location": "string",
                                "description": "string",
                                "category": "string",
                                "cost": "string",
                                "bookingUrl": "string"
                            }
                        ]
                    }
                ],
                "summary": "string"
            }
            """;
            
        String prompt = String.format("""
            You are an expert travel itinerary planner. Create a detailed day-by-day itinerary based on the following information:
            
            Trip Details:
            - Title: %s
            - Duration: %d days
            - Region: %s
            - People: %d
            - Interests: %s
            - Special Needs: Kids=%b, Elderly=%b, Accessible=%b
            - Weather Preference: %s
            - Notes: %s
            
            Available Resources:
            - Flights: %s
            - Hotels: %s
            - Transport: %s
            - Events/Activities: %s
            - Weather: %s
            
            Create a realistic itinerary that:
            1. Distributes activities across %d days with appropriate pacing
            2. Considers travel time between locations
            3. Balances different types of activities (sightseeing, dining, relaxation)
            4. Accounts for special needs and interests
            5. Includes practical details like check-in/out times
            6. Considers weather conditions for outdoor activities
            7. Provides realistic timing and durations
            
            Return ONLY valid JSON matching this schema:
            %s
            """,
            request.tripTitle != null ? request.tripTitle : "Travel Trip",
            request.days,
            request.region != null ? request.region : "Unknown",
            request.people,
            request.interests != null ? String.join(", ", request.interests) : "General",
            request.special != null ? request.special.kids : false,
            request.special != null ? request.special.elderly : false,
            request.special != null ? request.special.differentlyAbled : false,
            request.weatherPreference != null ? request.weatherPreference : "Any",
            request.notes != null ? request.notes : "None",
            flights != null ? flights.toString() : "None",
            hotels != null ? hotels.toString() : "None",
            transport != null ? transport.toString() : "None",
            events != null ? events.toString() : "None",
            weather != null ? weather.toString() : "None",
            request.days,
            schema
        );
        
        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(response -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(response);
                List<DayPlan> dayPlans = new ArrayList<>();
                
                JsonNode dayPlansNode = jsonNode.path("dayPlans");
                if (dayPlansNode.isArray()) {
                    for (JsonNode dayNode : dayPlansNode) {
                        DayPlan dayPlan = new DayPlan();
                        dayPlan.dayNumber = dayNode.path("dayNumber").asInt();
                        dayPlan.title = dayNode.path("title").asText();
                        dayPlan.activities = new ArrayList<>();
                        dayPlan.activitiesTyped = new ArrayList<>();
                        
                        JsonNode activitiesNode = dayNode.path("activities");
                        if (activitiesNode.isArray()) {
                            for (JsonNode activityNode : activitiesNode) {
                                Map<String, Object> activityMap = new java.util.HashMap<>();
                                activityMap.put("title", activityNode.path("title").asText());
                                activityMap.put("time", activityNode.path("time").asText());
                                activityMap.put("duration", activityNode.path("duration").asText());
                                activityMap.put("location", activityNode.path("location").asText());
                                activityMap.put("description", activityNode.path("description").asText());
                                activityMap.put("category", activityNode.path("category").asText());
                                activityMap.put("cost", activityNode.path("cost").asText());
                                activityMap.put("bookingUrl", activityNode.path("bookingUrl").asText());
                                
                                dayPlan.activities.add(activityMap);
                                
                                // Create typed activity
                                Activity activity = new Activity();
                                activity.title = activityNode.path("title").asText();
                                activity.time = activityNode.path("time").asText();
                                activity.details = activityMap;
                                dayPlan.activitiesTyped.add(activity);
                            }
                        }
                        
                        dayPlans.add(dayPlan);
                    }
                }
                
                return dayPlans;
                
            } catch (Exception e) {
                // Fallback to mock day plans if JSON parsing fails
                return createMockDayPlans(request.days);
            }
        });
    }
    
    public CompletableFuture<List<DayPlan>> refineDayPlans(
            TripRequest request,
            List<DayPlan> previousDayPlans,
            String amendments) {
        
        String schema = """
            {
                "dayPlans": [
                    {
                        "dayNumber": 1,
                        "title": "string",
                        "activities": [
                            {
                                "title": "string",
                                "time": "string",
                                "duration": "string",
                                "location": "string",
                                "description": "string",
                                "category": "string",
                                "cost": "string",
                                "bookingUrl": "string"
                            }
                        ]
                    }
                ],
                "summary": "string"
            }
            """;
            
        String previousItinerarySummary = previousDayPlans.stream()
                .map(dp -> String.format("Day %d: %s", dp.dayNumber, dp.title))
                .reduce((a, b) -> a + "; " + b)
                .orElse("No previous itinerary");
        
        String prompt = String.format("""
            You are an expert travel itinerary planner. Refine the following itinerary based on user feedback:
            
            Previous Itinerary:
            %s
            
            User Amendments/Feedback:
            %s
            
            Trip Details:
            - Title: %s
            - Duration: %d days
            - Region: %s
            - People: %d
            - Interests: %s
            
            Please adjust the itinerary according to the user's feedback while maintaining a realistic and well-paced schedule.
            
            Return ONLY valid JSON matching this schema:
            %s
            """,
            previousItinerarySummary,
            amendments != null ? amendments : "No specific feedback",
            request.tripTitle != null ? request.tripTitle : "Travel Trip",
            request.days,
            request.region != null ? request.region : "Unknown",
            request.people,
            request.interests != null ? String.join(", ", request.interests) : "General",
            schema
        );
        
        return llm.prompt(prompt, "gpt-3.5-turbo").thenApply(response -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(response);
                List<DayPlan> dayPlans = new ArrayList<>();
                
                JsonNode dayPlansNode = jsonNode.path("dayPlans");
                if (dayPlansNode.isArray()) {
                    for (JsonNode dayNode : dayPlansNode) {
                        DayPlan dayPlan = new DayPlan();
                        dayPlan.dayNumber = dayNode.path("dayNumber").asInt();
                        dayPlan.title = dayNode.path("title").asText();
                        dayPlan.activities = new ArrayList<>();
                        dayPlan.activitiesTyped = new ArrayList<>();
                        
                        JsonNode activitiesNode = dayNode.path("activities");
                        if (activitiesNode.isArray()) {
                            for (JsonNode activityNode : activitiesNode) {
                                Map<String, Object> activityMap = new java.util.HashMap<>();
                                activityMap.put("title", activityNode.path("title").asText());
                                activityMap.put("time", activityNode.path("time").asText());
                                activityMap.put("duration", activityNode.path("duration").asText());
                                activityMap.put("location", activityNode.path("location").asText());
                                activityMap.put("description", activityNode.path("description").asText());
                                activityMap.put("category", activityNode.path("category").asText());
                                activityMap.put("cost", activityNode.path("cost").asText());
                                activityMap.put("bookingUrl", activityNode.path("bookingUrl").asText());
                                
                                dayPlan.activities.add(activityMap);
                                
                                // Create typed activity
                                Activity activity = new Activity();
                                activity.title = activityNode.path("title").asText();
                                activity.time = activityNode.path("time").asText();
                                activity.details = activityMap;
                                dayPlan.activitiesTyped.add(activity);
                            }
                        }
                        
                        dayPlans.add(dayPlan);
                    }
                }
                
                return dayPlans;
                
            } catch (Exception e) {
                // Fallback to previous day plans if refinement fails
                return previousDayPlans;
            }
        });
    }
    
    private List<DayPlan> createMockDayPlans(int days) {
        List<DayPlan> mockPlans = new ArrayList<>();
        
        for (int i = 1; i <= days; i++) {
            DayPlan dayPlan = new DayPlan();
            dayPlan.dayNumber = i;
            dayPlan.title = "Day " + i + " Activities";
            dayPlan.activities = new ArrayList<>();
            dayPlan.activitiesTyped = new ArrayList<>();
            
            // Add mock activities
            Map<String, Object> morningActivity = Map.of(
                "title", "Morning Activity " + i,
                "time", "09:00",
                "duration", "2h",
                "location", "City Center",
                "description", "Explore local attractions",
                "category", "Sightseeing",
                "cost", "Free",
                "bookingUrl", "https://example.com"
            );
            
            Map<String, Object> afternoonActivity = Map.of(
                "title", "Afternoon Activity " + i,
                "time", "14:00",
                "duration", "3h",
                "location", "Historic District",
                "description", "Cultural experience",
                "category", "Culture",
                "cost", "25 USD",
                "bookingUrl", "https://example.com"
            );
            
            dayPlan.activities.add(morningActivity);
            dayPlan.activities.add(afternoonActivity);
            
            // Create typed activities
            Activity morningTyped = new Activity();
            morningTyped.title = "Morning Activity " + i;
            morningTyped.time = "09:00";
            morningTyped.details = morningActivity;
            dayPlan.activitiesTyped.add(morningTyped);
            
            Activity afternoonTyped = new Activity();
            afternoonTyped.title = "Afternoon Activity " + i;
            afternoonTyped.time = "14:00";
            afternoonTyped.details = afternoonActivity;
            dayPlan.activitiesTyped.add(afternoonTyped);
            
            mockPlans.add(dayPlan);
        }
        
        return mockPlans;
    }
}
