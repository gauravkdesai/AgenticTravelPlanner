package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TransportAgent {
    private final LLMClient llm;

    public TransportAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
    String amendments = safeGetAmendments(request);
    Object dates = safeGetTentativeDates(request);
    String schema = "{\n  \"carRental\": {\"provider\":string, \"price\":string, \"notes\":string},\n  \"trainOptions\": [{\"provider\":string, \"duration\":string, \"notes\":string}]\n}";
    String prompt = "You are a market-aware transport assistant. Given trip to " + request.region +
        ", tentativeDates='" + (dates==null?"":dates.toString()) + "', for " + request.people + " people and preferences " + (request.bookingPreferences==null?"":request.bookingPreferences) +
        ". Proactively consider seasonal routes, ferries, or cruise-related transport that may affect options and pricing. If user amendments: '" + (amendments==null?"":amendments) + "' include them in consideration. Return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add any extra commentary.";

        return llm.prompt(prompt, "gemini-pro-vision").thenApply(resp -> Map.of(
                "carRental", Map.of("provider","LLM RentACar","price","100 USD/day","notes",resp),
                "trainOptions", java.util.List.of(Map.of("provider","LLM Rail","duration","2h","notes",resp))
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
