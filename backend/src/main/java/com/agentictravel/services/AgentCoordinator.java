package com.agentictravel.services;

import com.agentictravel.model.*;
import com.agentictravel.security.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.agentictravel.llm.LLMClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(AgentCoordinator.class);

    private final FlightAgent flightAgent;
    private final TransportAgent transportAgent;
    private final HotelAgent hotelAgent;
    private final EventAgent eventAgent;
    private final WeatherAgent weatherAgent;
    private final QuestionAgent questionAgent;
    private final ItineraryPlannerAgent plannerAgent;

    public AgentCoordinator(LLMClient llm) {
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

    public CompletableFuture<Itinerary> generateItinerary(TripRequest request) {
        if (request.getAmendments() != null && !request.getAmendments().trim().isEmpty() &&
                request.getPreviousItinerary() != null) {
            return refineItinerary(request);
        }

        CompletableFuture<Map<String, Object>> flightsFuture = flightAgent.search(request);
        CompletableFuture<Map<String, Object>> transportFuture = transportAgent.search(request);
        CompletableFuture<Map<String, Object>> hotelsFuture = hotelAgent.search(request);
        CompletableFuture<List<Map<String, Object>>> eventsFuture = eventAgent.search(request);
        CompletableFuture<Map<String, Object>> weatherFuture = weatherAgent.search(request);

        return CompletableFuture.allOf(flightsFuture, transportFuture, hotelsFuture, eventsFuture, weatherFuture)
                .thenCompose(v -> {
                    try {
                        Map<String, Object> flights = flightsFuture.join();
                        Map<String, Object> transport = transportFuture.join();
                        Map<String, Object> hotels = hotelsFuture.join();
                        List<Map<String, Object>> events = eventsFuture.join();
                        Map<String, Object> weather = weatherFuture.join();

                        return plannerAgent.createDayPlans(request, flights, hotels, transport, events, weather)
                                .thenApply(dayPlans -> {
                                    Itinerary itinerary = new Itinerary();
                                    itinerary.summary = "Complete itinerary for " + request.tripTitle;
                                    itinerary.dayPlans = dayPlans;
                                    itinerary.notesParsingErrors = new java.util.ArrayList<>();

                                    try {
                                        itinerary.bookings = LLMToModelMapper.mapToBooking(flights, transport, hotels);
                                        itinerary.events = LLMToModelMapper.mapToEvents(events);
                                        itinerary.weather = LLMToModelMapper.mapToWeather(weather);

                                    } catch (Exception e) {
                                        LOG.warn("Failed to map itinerary components: {}", e.getMessage());
                                        itinerary.notesParsingErrors.add("Component mapping: " + e.getMessage());
                                    }

                                    return itinerary;
                                });
                    } catch (Exception e) {
                        LOG.error("Error in itinerary generation: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to generate itinerary", e);
                    }
                });
    }

    private CompletableFuture<Itinerary> refineItinerary(TripRequest request) {
        LOG.info("Refining itinerary based on amendments: {}", request.getAmendments());

        CompletableFuture<Map<String, Object>> flightsFuture = flightAgent.search(request);
        CompletableFuture<Map<String, Object>> transportFuture = transportAgent.search(request);
        CompletableFuture<Map<String, Object>> hotelsFuture = hotelAgent.search(request);
        CompletableFuture<List<Map<String, Object>>> eventsFuture = eventAgent.search(request);
        CompletableFuture<Map<String, Object>> weatherFuture = weatherAgent.search(request);

        return CompletableFuture.allOf(flightsFuture, transportFuture, hotelsFuture, eventsFuture, weatherFuture)
                .thenCompose(v -> {
                    try {
                        Map<String, Object> flights = flightsFuture.join();
                        Map<String, Object> transport = transportFuture.join();
                        Map<String, Object> hotels = hotelsFuture.join();
                        List<Map<String, Object>> events = eventsFuture.join();
                        Map<String, Object> weather = weatherFuture.join();

                        return plannerAgent.refineDayPlans(request, request.getPreviousItinerary().dayPlans, request.getAmendments())
                                .thenApply(dayPlans -> {
                                    Itinerary itinerary = new Itinerary();
                                    itinerary.summary = "Refined itinerary for " + request.tripTitle;
                                    itinerary.dayPlans = dayPlans;
                                    itinerary.notesParsingErrors = new java.util.ArrayList<>();

                                    try {
                                        itinerary.bookings = LLMToModelMapper.mapToBooking(flights, transport, hotels);
                                        itinerary.events = LLMToModelMapper.mapToEvents(events);
                                        itinerary.weather = LLMToModelMapper.mapToWeather(weather);

                                    } catch (Exception e) {
                                        LOG.warn("Failed to map refined itinerary components: {}", e.getMessage());
                                        itinerary.notesParsingErrors.add("Component mapping: " + e.getMessage());
                                    }

                                    return itinerary;
                                });
                    } catch (Exception e) {
                        LOG.error("Error in itinerary refinement: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to refine itinerary", e);
                    }
                });
    }
}