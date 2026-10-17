package org.example.ridehailing.ride_hailing.service;

import org.example.ridehailing.ride_hailing.dto.request.CreateTripRequest;
import org.example.ridehailing.ride_hailing.dto.request.UpdateDriverStatusRequest;
import org.example.ridehailing.ride_hailing.dto.request.UpdateTripStatusRequest;
import org.example.ridehailing.ride_hailing.dto.response.TripResponse;
import org.example.ridehailing.ride_hailing.dto.response.UserResponse;

import java.util.List;

public interface TripService {

    TripResponse createTrip(CreateTripRequest request);

    TripResponse getTripById(Long tripId);

    List<TripResponse> getCustomerTripHistory(Long customerId);

    UserResponse updateDriverStatus(UpdateDriverStatusRequest request);

    TripResponse acceptTrip(Long tripId, Long driverId);

    TripResponse updateTripStatus(Long tripId, UpdateTripStatusRequest request);
}