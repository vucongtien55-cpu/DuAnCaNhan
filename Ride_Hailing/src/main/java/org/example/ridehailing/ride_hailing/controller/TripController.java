package org.example.ridehailing.ride_hailing.controller;

import org.example.ridehailing.ride_hailing.dto.request.CreateTripRequest;
import org.example.ridehailing.ride_hailing.dto.request.UpdateDriverStatusRequest;
import org.example.ridehailing.ride_hailing.dto.request.UpdateTripStatusRequest;
import org.example.ridehailing.ride_hailing.dto.response.TripResponse;
import org.example.ridehailing.ride_hailing.dto.response.UserResponse;
import org.example.ridehailing.ride_hailing.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable("id") Long id) {
        TripResponse response = tripService.getTripById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<TripResponse>> getCustomerHistory(@PathVariable("customerId") Long customerId) {
        List<TripResponse> responses = tripService.getCustomerTripHistory(customerId);
        return ResponseEntity.ok(responses);
    }

    // --- API BẮT BUỘC PHẢI CÓ DƯỚI ĐÂY ---
    @PutMapping("/driver/status")
    public ResponseEntity<UserResponse> updateDriverStatus(@Valid @RequestBody UpdateDriverStatusRequest request) {
        UserResponse response = tripService.updateDriverStatus(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tripId}/accept")
    public ResponseEntity<TripResponse> acceptTrip(
            @PathVariable("tripId") Long tripId,
            @RequestParam("driverId") Long driverId) {
        TripResponse response = tripService.acceptTrip(tripId, driverId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tripId}/status")
    public ResponseEntity<TripResponse> updateTripStatus(
            @PathVariable("tripId") Long tripId,
            @Valid @RequestBody UpdateTripStatusRequest request) {
        TripResponse response = tripService.updateTripStatus(tripId, request);
        return ResponseEntity.ok(response);
    }
}