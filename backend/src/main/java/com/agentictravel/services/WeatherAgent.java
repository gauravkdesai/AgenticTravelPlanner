package com.agentictravel.services;

import com.agentictravel.llm.LLMClient;
import com.agentictravel.model.TripRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WeatherAgent {
    private final LLMClient llm;

    public WeatherAgent(LLMClient llm){
        this.llm = llm;
    }

    public CompletableFuture<Map<String,Object>> search(TripRequest request){
    String amendments = safeGetAmendments(request);
    Object dates = safeGetTentativeDates(request);
    String schema = "{ \"forecastSummary\": string, \"high\": string, \"low\": string }";
    String prompt = "You are a weather assistant. For region " + request.region + ", tentativeDates='" + (dates==null?"":dates.toString()) + "' and user amendments '" + (amendments==null?"":amendments) + "', return ONLY valid JSON strictly matching this schema: \n" + schema + "\nDo not add commentary outside the JSON.";

        return llm.prompt(prompt, "gemini-pro-vision").thenApply(resp -> Map.of(
                "forecastSummary",resp,
                "high","18 C",
                "low","10 C"
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
