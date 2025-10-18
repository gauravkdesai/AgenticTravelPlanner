package com.agentictravel.services;

import com.agentictravel.model.Booking;
// no unused imports

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Small, defensive mapper that converts loosely-typed LLM output (maps/lists)
 * into the project's typed model classes. It validates presence of expected keys
 * and falls back gracefully when fields are missing or mal-typed.
 */
public class LLMToModelMapper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LLMToModelMapper.class);

    public static Booking mapToBooking(Map<String,Object> flightsRaw, Map<String,Object> transportRaw, Map<String,Object> hotelsRaw){
        Booking b = new Booking();
        b.flights = safeCopyMap(flightsRaw);
        b.transport = safeCopyMap(transportRaw);
        b.hotels = safeCopyMap(hotelsRaw);
        // populate typed flight booking
        try{
            if(flightsRaw!=null){
                Object rec = flightsRaw.get("recommended");
                if(rec instanceof Map){
                    @SuppressWarnings("unchecked")
                    Map<String,Object> recm = (Map<String,Object>) rec;
                    com.agentictravel.model.FlightBooking fb = new com.agentictravel.model.FlightBooking();
                    fb.carrier = safeGetString(recm, "carrier");
                    fb.price = safeGetString(recm, "price");
                    Object notes = recm.get("notes_parsed");
                    if(notes instanceof Map) fb.notes = safeCopyMap((Map<String,Object>) notes);
                    b.flightsTyped = fb;
                }
            }
        }catch(Exception ex){ /* non-fatal */ }

        // populate typed transport booking
        try{
            if(transportRaw!=null){
                Object rec = transportRaw.get("recommended");
                if(rec instanceof Map){
                    @SuppressWarnings("unchecked")
                    Map<String,Object> recm = (Map<String,Object>) rec;
                    com.agentictravel.model.TransportBooking tb = new com.agentictravel.model.TransportBooking();
                    tb.provider = safeGetString(recm, "provider");
                    tb.price = safeGetString(recm, "price");
                    Object notes = recm.get("notes_parsed");
                    if(notes instanceof Map) tb.notes = safeCopyMap((Map<String,Object>) notes);
                    b.transportTyped = tb;
                }
            }
        }catch(Exception ex){ /* non-fatal */ }

        // populate typed hotel booking
        try{
            if(hotelsRaw!=null){
                Object rec = hotelsRaw.get("recommended");
                if(rec instanceof Map){
                    @SuppressWarnings("unchecked")
                    Map<String,Object> recm = (Map<String,Object>) rec;
                    com.agentictravel.model.HotelBooking hb = new com.agentictravel.model.HotelBooking();
                    hb.name = safeGetString(recm, "name");
                    hb.price = safeGetString(recm, "price");
                    Object notes = recm.get("notes_parsed");
                    if(notes instanceof Map) hb.notes = safeCopyMap((Map<String,Object>) notes);
                    b.hotelsTyped = hb;
                }
            }
        }catch(Exception ex){ /* non-fatal */ }
        return b;
    }

    public static List<Map<String,Object>> mapToEvents(List<Map<String,Object>> eventsRaw){
        if(eventsRaw==null) return List.of();
        List<Map<String,Object>> out = new ArrayList<>();
        for(Map<String,Object> e : eventsRaw){
            out.add(safeCopyMap(e));
        }
        return out;
    }

    public static Map<String,Object> mapToWeather(Map<String,Object> weatherRaw){
        return safeCopyMap(weatherRaw);
    }

    /**
     * Map a list of day plan maps into typed DayPlan objects with typed activities.
     * Returns null or empty list when input is invalid.
     */
    public static java.util.List<com.agentictravel.model.DayPlan> mapToDayPlans(Object dayPlansObj){
        if(dayPlansObj==null) return java.util.List.of();
        try{
            if(!(dayPlansObj instanceof java.util.List)) return java.util.List.of();
            java.util.List<?> raw = (java.util.List<?>) dayPlansObj;
            java.util.List<com.agentictravel.model.DayPlan> out = new java.util.ArrayList<>();
            for(Object item : raw){
                if(!(item instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String,Object> m = (java.util.Map<String,Object>) item;
                com.agentictravel.model.DayPlan dp = new com.agentictravel.model.DayPlan();
                dp.dayNumber = safeGetInt(m, "dayNumber");
                dp.title = safeGetString(m, "title");
                // activities
                Object acts = m.get("activities");
                dp.activities = acts instanceof java.util.List ? (java.util.List<java.util.Map<String,Object>>) acts : java.util.List.of();
                dp.activitiesTyped = new java.util.ArrayList<>();
                for(Object a : dp.activities){
                    if(a instanceof java.util.Map){
                        @SuppressWarnings("unchecked") java.util.Map<String,Object> am = (java.util.Map<String,Object>) a;
                        com.agentictravel.model.Activity act = new com.agentictravel.model.Activity();
                        act.title = safeGetString(am, "title");
                        act.time = safeGetString(am, "time");
                        act.details = safeCopyMap(am);
                        dp.activitiesTyped.add(act);
                    }
                }
                out.add(dp);
            }
            return out;
        }catch(Exception e){
            log.debug("Failed to map dayPlans: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    // Defensive shallow copy ensures maps are mutable and of correct generic type
    private static Map<String,Object> safeCopyMap(Map<String,Object> m){
        if(m==null) return java.util.Map.of();
        return new java.util.HashMap<>(m);
    }

    private static String safeGetString(Map<String,Object> m, String key){
        if(m==null) return null;
        Object v = m.get(key);
        return v==null?null:v.toString();
    }

    private static int safeGetInt(Map<String,Object> m, String key){
        if(m==null) return 0;
        Object v = m.get(key);
        if(v instanceof Number) return ((Number)v).intValue();
        try{
            return Integer.parseInt(v.toString());
        }catch(Exception e){
            return 0;
        }
    }
}
