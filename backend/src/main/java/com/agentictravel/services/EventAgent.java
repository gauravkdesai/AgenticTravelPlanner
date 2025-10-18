package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EventAgent {
    private final LLMClient llm;

    public EventAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<List<Map<String,Object>>> search(TripRequest request){
    String amendments = safeGetAmendments(request);
    Object dates = safeGetTentativeDates(request);
    String schema = "[ {\"name\":string, \"date\":string, \"location\":string, \"description\":string} ]";
    String prompt = "You are a market-aware events assistant. Given region " + request.region +
        ", tentative dates '" + (dates==null?"":dates.toString()) + "' and user amendments '" + (amendments==null?"":amendments) + "', proactively search for public events, festivals, or cruises occurring during those dates that would be relevant to the traveler. Return ONLY a valid JSON array strictly matching this schema: \n" + schema + "\nDo not add any commentary outside the JSON.";

        return llm.prompt(prompt, "gemini-pro-vision").thenApply(resp -> List.of(
                Map.of("name","LLM Food Fest","date","2025-10-25","location",request.region,"description",resp),
                Map.of("name","LLM Concert","date","2025-10-26","location",request.region,"description",resp)
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
