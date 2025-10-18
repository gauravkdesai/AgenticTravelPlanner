package com.agentictravel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for agent behavior and timeouts.
 * Maps to agent.* properties in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {
    
    private FlightAgentConfig flight = new FlightAgentConfig();
    private HotelAgentConfig hotel = new HotelAgentConfig();
    private TransportAgentConfig transport = new TransportAgentConfig();
    private EventAgentConfig event = new EventAgentConfig();
    private WeatherAgentConfig weather = new WeatherAgentConfig();
    private QuestionAgentConfig question = new QuestionAgentConfig();
    private PlannerAgentConfig planner = new PlannerAgentConfig();
    
    public FlightAgentConfig getFlight() {
        return flight;
    }
    
    public void setFlight(FlightAgentConfig flight) {
        this.flight = flight;
    }
    
    public HotelAgentConfig getHotel() {
        return hotel;
    }
    
    public void setHotel(HotelAgentConfig hotel) {
        this.hotel = hotel;
    }
    
    public TransportAgentConfig getTransport() {
        return transport;
    }
    
    public void setTransport(TransportAgentConfig transport) {
        this.transport = transport;
    }
    
    public EventAgentConfig getEvent() {
        return event;
    }
    
    public void setEvent(EventAgentConfig event) {
        this.event = event;
    }
    
    public WeatherAgentConfig getWeather() {
        return weather;
    }
    
    public void setWeather(WeatherAgentConfig weather) {
        this.weather = weather;
    }
    
    public QuestionAgentConfig getQuestion() {
        return question;
    }
    
    public void setQuestion(QuestionAgentConfig question) {
        this.question = question;
    }
    
    public PlannerAgentConfig getPlanner() {
        return planner;
    }
    
    public void setPlanner(PlannerAgentConfig planner) {
        this.planner = planner;
    }
    
    public static class FlightAgentConfig {
        private int maxOptions = 5;
        private int timeout = 30;
        
        public int getMaxOptions() {
            return maxOptions;
        }
        
        public void setMaxOptions(int maxOptions) {
            this.maxOptions = maxOptions;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class HotelAgentConfig {
        private int maxOptions = 5;
        private int timeout = 30;
        
        public int getMaxOptions() {
            return maxOptions;
        }
        
        public void setMaxOptions(int maxOptions) {
            this.maxOptions = maxOptions;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class TransportAgentConfig {
        private int maxOptions = 5;
        private int timeout = 30;
        
        public int getMaxOptions() {
            return maxOptions;
        }
        
        public void setMaxOptions(int maxOptions) {
            this.maxOptions = maxOptions;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class EventAgentConfig {
        private int maxOptions = 10;
        private int timeout = 30;
        
        public int getMaxOptions() {
            return maxOptions;
        }
        
        public void setMaxOptions(int maxOptions) {
            this.maxOptions = maxOptions;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class WeatherAgentConfig {
        private int timeout = 30;
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class QuestionAgentConfig {
        private int maxQuestions = 4;
        private int timeout = 30;
        
        public int getMaxQuestions() {
            return maxQuestions;
        }
        
        public void setMaxQuestions(int maxQuestions) {
            this.maxQuestions = maxQuestions;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class PlannerAgentConfig {
        private int timeout = 60;
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
}
