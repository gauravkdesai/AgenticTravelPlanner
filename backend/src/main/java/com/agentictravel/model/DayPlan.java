package com.agentictravel.model;

import java.util.List;
import java.util.Map;
import com.agentictravel.model.Activity;

public class DayPlan {
    public int dayNumber;
    public String title;
    public List<Map<String,Object>> activities; // simple generic activities
    // Typed activities for programmatic consumption
    public java.util.List<Activity> activitiesTyped;
}
