package com.agentictravel.services;

import com.agentictravel.model.Booking;
import com.agentictravel.model.DayPlan;
import com.agentictravel.model.Itinerary;
import com.agentictravel.model.TripRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LLMToModelMapper {

    private static final Logger LOG = LoggerFactory.getLogger(LLMToModelMapper.class);

    public static Booking mapToBooking(Map<String, Object> flightsRaw, Map<String, Object> transportRaw, Map<String, Object> hotelsRaw) {
        Booking b = new Booking();
        b.flights = safeCopyMap(flightsRaw);
        b.transport = safeCopyMap(transportRaw);
        b.hotels = safeCopyMap(hotelsRaw);
        return b;
    }

    public static List<Map<String, Object>> mapToEvents(List<Map<String, Object>> eventsRaw) {
        if (eventsRaw == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> e : eventsRaw) {
            out.add(safeCopyMap(e));
        }
        return out;
    }

    public static Map<String, Object> mapToWeather(Map<String, Object> weatherRaw) {
        return safeCopyMap(weatherRaw);
    }

    public static List<DayPlan> mapToDayPlans(Object dayPlansObj) {
        if (dayPlansObj == null) return List.of();
        try {
            if (!(dayPlansObj instanceof java.util.List)) return List.of();
            java.util.List<?> raw = (java.util.List<?>) dayPlansObj;
            java.util.List<DayPlan> out = new java.util.ArrayList<>();
            for (Object item : raw) {
                if (!(item instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) item;
                DayPlan dp = new DayPlan();
                dp.dayNumber = safeGetInt(m, "dayNumber");
                dp.title = safeGetString(m, "title");
                Object acts = m.get("activities");
                dp.activities = acts instanceof java.util.List ? (java.util.List<java.util.Map<String, Object>>) acts : java.util.List.of();
                out.add(dp);
            }
            return out;
        } catch (Exception e) {
            LOG.debug("Failed to map dayPlans: {}", e.getMessage());
            return List.of();
        }
    }

    private static Map<String, Object> safeCopyMap(Map<String, Object> m) {
        if (m == null) return java.util.Map.of();
        java.util.Map<String,Object> out = new java.util.HashMap<>(m);
        // Recursively process nested maps/lists to parse any 'notes' strings into notes_parsed
        processNestedForNotes(out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void processNestedForNotes(java.util.Map<String,Object> map) {
        ObjectMapper om = new ObjectMapper();
        for (java.util.Map.Entry<String,Object> e : new java.util.ArrayList<>(map.entrySet())) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val instanceof java.util.Map) {
                processNestedForNotes((java.util.Map<String,Object>) val);
            } else if (val instanceof java.util.List) {
                java.util.List<?> lst = (java.util.List<?>) val;
                for (Object item : lst) {
                    if (item instanceof java.util.Map) {
                        processNestedForNotes((java.util.Map<String,Object>) item);
                    }
                }
            } else if ("notes".equalsIgnoreCase(key) && val instanceof String) {
                String s = ((String) val).trim();
                if (s.startsWith("{") || s.startsWith("[")) {
                    try {
                        Object parsed = om.readValue(s, Object.class);
                        map.put("notes_parsed", parsed);
                    } catch (Exception ex) {
                        LOG.debug("notes field not valid JSON: {}", ex.getMessage());
                    }
                }
            }
        }
    }

    private static String safeGetString(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private static int safeGetInt(Map<String, Object> m, String key) {
        if (m == null) return 0;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
