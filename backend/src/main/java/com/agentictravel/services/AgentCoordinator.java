package com.agentictravel.services;

import com.agentictravel.model.Itinerary;
import com.agentictravel.model.QuestionResponse;
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
    private final QuestionAgent questionAgent;
    private final ItineraryPlannerAgent plannerAgent;

    public AgentCoordinator(com.agentictravel.llm.LLMClient llm){
        this.flightAgent = new FlightAgent(llm);
        this.transportAgent = new TransportAgent(llm);
        this.hotelAgent = new HotelAgent(llm);
        this.eventAgent = new EventAgent(llm);
        this.weatherAgent = new WeatherAgent(llm);
        this.questionAgent = new QuestionAgent(llm);
        this.plannerAgent = new ItineraryPlannerAgent(llm);
    }

    public CompletableFuture<QuestionResponse> generateQuestions(TripRequest request) {
        return questionAgent.generateQuestions(request);
    }

    public CompletableFuture<Itinerary> generateItinerary(TripRequest request){
        // Check if this is a refinement request
        if (request.amendments != null && !request.amendments.trim().isEmpty() && 
            request.previousItinerary != null) {
            return refineItinerary(request);
        }
        
        // Run agents in parallel and aggregate a complete Itinerary
        CompletableFuture<Map<String,Object>> flightsFuture = flightAgent.search(request);
        CompletableFuture<Map<String,Object>> transportFuture = transportAgent.search(request);
        CompletableFuture<Map<String,Object>> hotelsFuture = hotelAgent.search(request);
        CompletableFuture<List<Map<String,Object>>> eventsFuture = eventAgent.search(request);
        CompletableFuture<Map<String,Object>> weatherFuture = weatherAgent.search(request);

        return CompletableFuture.allOf(flightsFuture, transportFuture, hotelsFuture, eventsFuture, weatherFuture)
                .thenCompose(v -> {
                    try {
                        Map<String,Object> flights = flightsFuture.join();
                        Map<String,Object> transport = transportFuture.join();
                        Map<String,Object> hotels = hotelsFuture.join();
                        List<Map<String,Object>> events = eventsFuture.join();
                        Map<String,Object> weather = weatherFuture.join();
                        
                        // Create day-by-day itinerary using the planner agent
                        return plannerAgent.createDayPlans(request, flights, hotels, transport, events, weather)
                                .thenApply(dayPlans -> {
                                    Itinerary itinerary = new Itinerary();
                                    itinerary.summary = "Complete itinerary for " + request.tripTitle;
                                    itinerary.dayPlans = dayPlans;
                                    itinerary.notesParsingErrors = new java.util.ArrayList<>();
                                    
                                    try {
                                        // Map bookings with multiple options
                                        Booking bookings = new Booking();
                                        bookings.flights = flights;
                                        bookings.transport = transport;
                                        bookings.hotels = hotels;
                                        itinerary.bookings = bookings;
                                        
                                        // Map events
                                        itinerary.events = events;
                                        
                                        // Map weather
                                        itinerary.weather = weather;
                                        
                                    } catch (Exception e) {
                                        log.warn("Failed to map itinerary components: {}", e.getMessage());
                                        itinerary.notesParsingErrors.add("Component mapping: " + e.getMessage());
                                    }
                                    
                                    return itinerary;
                                });
                    } catch (Exception e) {
                        log.error("Error in itinerary generation: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to generate itinerary", e);
                    }
                });
    }
    
    private CompletableFuture<Itinerary> refineItinerary(TripRequest request) {
        log.info("Refining itinerary based on amendments: {}", request.amendments);
        
        // For refinement, we can either re-run all agents or just the planner
        // For now, let's re-run all agents to get fresh options
        CompletableFuture<Map<String,Object>> flightsFuture = flightAgent.search(request);
        CompletableFuture<Map<String,Object>> transportFuture = transportAgent.search(request);
        CompletableFuture<Map<String,Object>> hotelsFuture = hotelAgent.search(request);
        CompletableFuture<List<Map<String,Object>>> eventsFuture = eventAgent.search(request);
        CompletableFuture<Map<String,Object>> weatherFuture = weatherAgent.search(request);

        return CompletableFuture.allOf(flightsFuture, transportFuture, hotelsFuture, eventsFuture, weatherFuture)
                .thenCompose(v -> {
                    try {
                        Map<String,Object> flights = flightsFuture.join();
                        Map<String,Object> transport = transportFuture.join();
                        Map<String,Object> hotels = hotelsFuture.join();
                        List<Map<String,Object>> events = eventsFuture.join();
                        Map<String,Object> weather = weatherFuture.join();
                        
                        // Refine the day plans based on amendments
                        return plannerAgent.refineDayPlans(request, request.previousItinerary.dayPlans, request.amendments)
                                .thenApply(dayPlans -> {
                                    Itinerary itinerary = new Itinerary();
                                    itinerary.summary = "Refined itinerary for " + request.tripTitle;
                                    itinerary.dayPlans = dayPlans;
                                    itinerary.notesParsingErrors = new java.util.ArrayList<>();
                                    
                                    try {
                                        // Map bookings with multiple options
                                        Booking bookings = new Booking();
                                        bookings.flights = flights;
                                        bookings.transport = transport;
                                        bookings.hotels = hotels;
                                        itinerary.bookings = bookings;
                                        
                                        // Map events
                                        itinerary.events = events;
                                        
                                        // Map weather
                                        itinerary.weather = weather;
                                        
                                    } catch (Exception e) {
                                        log.warn("Failed to map refined itinerary components: {}", e.getMessage());
                                        itinerary.notesParsingErrors.add("Component mapping: " + e.getMessage());
                                    }
                                    
                                    return itinerary;
                                });
                    } catch (Exception e) {
                        log.error("Error in itinerary refinement: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to refine itinerary", e);
                    }
                });
    }
}
