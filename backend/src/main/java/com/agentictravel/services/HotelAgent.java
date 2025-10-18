package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HotelAgent {
    private final LLMClient llm;

    public HotelAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
    String amendments = safeGetAmendments(request);
    Object dates = safeGetTentativeDates(request);
    String schema = "{\n  \"recommended\": {\"name\":string, \"pricePerNight\":string, \"notes\":string},\n  \"availabilityNote\": string\n}";
    String prompt = "You are a hotel search assistant aware of market events. Given the trip: " + request.tripTitle +
        ", region=" + request.region + ", tentativeDates='" + (dates==null?"":dates.toString()) + "', nights=" + Math.max(request.days,1) +
        ". Proactively consider events, cruises, or conferences that may affect availability/pricing and mention them in notes. If the user provided amendments: '" + (amendments==null?"":amendments) + "' include them when suggesting hotels. Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add any commentary outside the JSON.";

        return llm.prompt(prompt, "gemini-pro-vision").thenApply(resp -> Map.of(
                "recommended", Map.of("name","LLM Hotel","pricePerNight","180 USD","notes",resp),
                "availabilityNote","Checked via LLM"
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
