package com.agentictravel.model;

import java.util.Map;
import com.agentictravel.model.FlightBooking;
import com.agentictravel.model.HotelBooking;
import com.agentictravel.model.TransportBooking;

public class Booking {
    public Map<String,Object> flights;
    public Map<String,Object> transport;
    public Map<String,Object> hotels;
    // Typed representations (optional)
    public FlightBooking flightsTyped;
    public TransportBooking transportTyped;
    public HotelBooking hotelsTyped;
}
