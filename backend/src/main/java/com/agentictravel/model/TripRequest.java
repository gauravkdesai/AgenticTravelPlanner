package com.agentictravel.model;

import java.util.List;
import com.agentictravel.model.Itinerary;

public class TripRequest {
    public String tripTitle;
    public int days;
    public String region;
    public String budget;
    public int people;
    public Special special;
    public String weatherPreference;
    public List<String> foodPreferences;
    public List<String> bookingPreferences;
    public List<String> interests;
    // Optional tentative dates (ISO date range or list). Example: "2025-12-20 to 2025-12-27" or ["2025-12-20", "2025-12-27"]
    public Object tentativeDates;
    public String amendments; // user textual suggestions like "make it 5 days" or "more relaxing on day 3"
    public Itinerary previousItinerary;
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
