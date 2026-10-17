package org.example.ridehailing.ride_hailing.repository;

import org.example.ridehailing.ride_hailing.entity.DeliveryInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryInfoRepository extends JpaRepository<DeliveryInfo, Long> {

    // Tìm thông tin kiện hàng theo ID chuyến xe
    Optional<DeliveryInfo> findByTripId(Long tripId);
}