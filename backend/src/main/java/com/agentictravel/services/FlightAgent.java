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
    String schema = "{\n  \"recommended\": {\"carrier\":string, \"price\":string, \"notes\":string},\n  \"alternatives\": [{\"carrier\":string, \"price\":string}]\n}";
    String prompt = "You are a market-aware travel assistant. Given the trip request: " + request.tripTitle +
    ", days=" + request.days + ", region=" + request.region + ", people=" + request.people +
    ". Tentative dates: '" + (dates==null?"":dates.toString()) + "'. If available in the market during those dates, proactively suggest special options (for example limited-time flights, seasonal routes, or cruise departures that affect flight availability)."
    + " If the user provided amendments: '" + (amendments==null?"":amendments) + "' include them when suggesting flights. Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add any extra commentary outside the JSON.";

        return llm.prompt(prompt, "gemini-pro-vision").thenApply(resp -> Map.of(
                "recommended", Map.of("carrier","LLM-SuggestedAir","price","500 USD","notes",resp),
                "alternatives", java.util.List.of(Map.of("carrier","AltAir","price","550 USD"))
        ));
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
