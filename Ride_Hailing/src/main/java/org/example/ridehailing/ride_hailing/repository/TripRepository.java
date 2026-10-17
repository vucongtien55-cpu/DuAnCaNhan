package org.example.ridehailing.ride_hailing.repository;

import org.example.ridehailing.ride_hailing.entity.Trip;
import org.example.ridehailing.ride_hailing.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Tìm lịch sử chuyến xe của khách hàng
    List<Trip> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Tìm các chuyến xe của tài xế theo trạng thái
    List<Trip> findByDriverIdAndStatus(Long driverId, TripStatus status);
}