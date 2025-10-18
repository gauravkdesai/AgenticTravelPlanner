package com.agentictravel.model;

import jakarta.validation.constraints.*;
import java.util.List;
import com.agentictravel.model.Itinerary;

public class TripRequest {
    
    @Size(max = 200, message = "Trip title must not exceed 200 characters")
    public String tripTitle;
    
    @Min(value = 1, message = "Number of days must be at least 1")
    @Max(value = 365, message = "Number of days cannot exceed 365")
    public int days;
    
    @Size(max = 100, message = "Region must not exceed 100 characters")
    public String region;
    
    @Size(max = 50, message = "Budget must not exceed 50 characters")
    public String budget;
    
    @Min(value = 1, message = "Number of people must be at least 1")
    @Max(value = 50, message = "Number of people cannot exceed 50")
    public int people;
    
    public Special special;
    
    @Pattern(regexp = "^(any|warm|mild|cool|cold|rainy)$", message = "Invalid weather preference")
    public String weatherPreference;
    
    @Size(max = 10, message = "Cannot have more than 10 food preferences")
    public List<String> foodPreferences;
    
    @Size(max = 4, message = "Cannot have more than 4 booking preferences")
    public List<String> bookingPreferences;
    
    @Size(max = 10, message = "Cannot have more than 10 interests")
    public List<String> interests;
    
    // Optional tentative dates (ISO date range or list). Example: "2025-12-20 to 2025-12-27" or ["2025-12-20", "2025-12-27"]
    public Object tentativeDates;
    
    @Size(max = 1000, message = "Amendments must not exceed 1000 characters")
    public String amendments; // user textual suggestions like "make it 5 days" or "more relaxing on day 3"
    
    public Itinerary previousItinerary;
    
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    public String notes;

    // simple accessors to help other modules avoid direct field access
    public String getAmendments() {
        return amendments;
    }

    public Itinerary getPreviousItinerary() {
        return previousItinerary;
    }

    public static class Special {
        public boolean kids;
        public boolean elderly;
        public boolean differentlyAbled;
    }
}
