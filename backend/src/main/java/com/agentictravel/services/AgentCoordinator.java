package com.agentictravel.services;

import com.agentictravel.model.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agentictravel.model.Booking;
import com.agentictravel.model.TripRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentCoordinator {
    private static final Logger log = LoggerFactory.getLogger(AgentCoordinator.class);
    private final FlightAgent flightAgent;
    private final TransportAgent transportAgent;
    private final HotelAgent hotelAgent;
    private final EventAgent eventAgent;
    private final WeatherAgent weatherAgent;

    public AgentCoordinator(com.agentictravel.llm.LLMClient llm){
        this.flightAgent = new FlightAgent(llm);
        this.transportAgent = new TransportAgent(llm);
        this.hotelAgent = new HotelAgent(llm);
        this.eventAgent = new EventAgent(llm);
        this.weatherAgent = new WeatherAgent(llm);
    }

    public CompletableFuture<Itinerary> generateItinerary(TripRequest request){
        // Run agents in parallel and aggregate a simple Itinerary
    CompletableFuture<java.util.Map<String,Object>> varFlights = flightAgent.search(request);
    CompletableFuture<java.util.Map<String,Object>> varTransport = transportAgent.search(request);
    CompletableFuture<java.util.Map<String,Object>> varHotels = hotelAgent.search(request);
    CompletableFuture<java.util.List<java.util.Map<String,Object>>> varEvents = eventAgent.search(request);
    CompletableFuture<java.util.Map<String,Object>> varWeather = weatherAgent.search(request);

        return CompletableFuture.allOf(varFlights, varTransport, varHotels, varEvents, varWeather)
                .thenApply(v -> {
                    Itinerary it = new Itinerary();
                    it.summary = "Sample itinerary for " + request.tripTitle;
                    it.dayPlans = java.util.List.of();
                    it.notesParsingErrors = new java.util.ArrayList<>();
                    try{
                        Booking bookings = new Booking();
                        Map<String,Object> flightsRaw = new java.util.HashMap<>(varFlights.join());
                        Map<String,Object> transportRaw = new java.util.HashMap<>(varTransport.join());
                        Map<String,Object> hotelsRaw = new java.util.HashMap<>(varHotels.join());

                        // map into typed booking object
                        try{
                            it.bookings = LLMToModelMapper.mapToBooking(flightsRaw, transportRaw, hotelsRaw);
                        }catch(Exception e){
                            log.warn("Failed to map bookings: {}", e.getMessage());
                            it.notesParsingErrors.add("bookings mapping: " + e.getMessage());
                            bookings.flights = flightsRaw;
                            bookings.transport = transportRaw;
                            bookings.hotels = hotelsRaw;
                            it.bookings = bookings;
                        }
                        // events may be returned as a JSON array string inside a note; try to parse
                        Object eventsObj = varEvents.join();
                        if(eventsObj instanceof List){
                            // ensure typed list with explicit casts to avoid inference errors
                            List<?> raw = (List<?>) eventsObj;
                            List<Map<String,Object>> em = new java.util.ArrayList<>();
                            for(Object o : raw){
                                if(o instanceof Map) em.add((Map<String,Object>) o);
                                else {
                                    java.util.Map<String,Object> tmp = new java.util.HashMap<>();
                                    tmp.put("value", o==null?null:o.toString());
                                    em.add(tmp);
                                }
                            }
                            try{
                                it.events = LLMToModelMapper.mapToEvents(em);
                            }catch(Exception e){
                                log.debug("event mapping failed: {}", e.getMessage());
                                it.notesParsingErrors.add("events mapping: " + e.getMessage());
                                it.events = em;
                            }
                        } else {
                            it.events = java.util.List.of();
                        }

                        Object weatherObj = varWeather.join();
                        try{
                            if(weatherObj instanceof Map){
                                it.weather = LLMToModelMapper.mapToWeather((Map<String,Object>) weatherObj);
                            } else {
                                it.weather = java.util.Map.of();
                            }
                        }catch(Exception e){
                            log.debug("weather mapping failed: {}", e.getMessage());
                            it.notesParsingErrors.add("weather mapping: " + e.getMessage());
                            it.weather = java.util.Map.of();
                        }
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                    return it;
                });
    }
}
