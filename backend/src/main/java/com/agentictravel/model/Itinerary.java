package com.agentictravel.model;

import java.util.List;

public class Itinerary {
    public String summary;
    public List<DayPlan> dayPlans;
    public Booking bookings;
    public java.util.Map<String,Object> weather;
    public List<java.util.Map<String,Object>> events;
    // Collect non-fatal parsing errors encountered while mapping LLM responses
    public List<String> notesParsingErrors;
}
