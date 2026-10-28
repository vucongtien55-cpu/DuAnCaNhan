package org.example.ridehailing.ride_hailing.service.impl;

import org.example.ridehailing.ride_hailing.dto.request.CreateTripRequest;
import org.example.ridehailing.ride_hailing.dto.request.UpdateDriverStatusRequest;
import org.example.ridehailing.ride_hailing.dto.request.UpdateTripStatusRequest;
import org.example.ridehailing.ride_hailing.dto.response.TripResponse;
import org.example.ridehailing.ride_hailing.dto.response.UserResponse;
import org.example.ridehailing.ride_hailing.entity.*;
import org.example.ridehailing.ride_hailing.repository.DeliveryInfoRepository;
import org.example.ridehailing.ride_hailing.repository.TripRepository;
import org.example.ridehailing.ride_hailing.repository.UserRepository;
import org.example.ridehailing.ride_hailing.service.TripService;
import org.example.ridehailing.ride_hailing.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final DeliveryInfoRepository deliveryInfoRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    private static final double EARTH_RADIUS_KM = 6371.0;

    // 1. Tạo chuyến xe
    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại!"));

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new RuntimeException("Chỉ có tài khoản Khách hàng mới được phép đặt chuyến!");
        }

        double distance = calculateDistanceKm(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng()
        );
        BigDecimal distanceKm = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);

        double surgeMultiplier = 1.0;
        BigDecimal price = calculatePrice(request.getServiceType(), distanceKm, surgeMultiplier);

        Trip trip = Trip.builder()
                .customerId(request.getCustomerId())
                .serviceType(request.getServiceType())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .pickupAddress(request.getPickupAddress())
                .dropoffLat(request.getDropoffLat())
                .dropoffLng(request.getDropoffLng())
                .dropoffAddress(request.getDropoffAddress())
                .distanceKm(distanceKm)
                .price(price)
                .surgeMultiplier(surgeMultiplier)
                .status(TripStatus.CREATED)
                .build();

        Trip savedTrip = tripRepository.save(trip);

        DeliveryInfo deliveryInfo = null;
        if (request.getServiceType() == ServiceType.DELIVERY) {
            deliveryInfo = DeliveryInfo.builder()
                    .tripId(savedTrip.getId())
                    .senderName(request.getSenderName())
                    .senderPhone(request.getSenderPhone())
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .packageName(request.getPackageName())
                    .weightKg(request.getWeightKg())
                    .noteForDriver(request.getNoteForDriver())
                    .build();
            deliveryInfoRepository.save(deliveryInfo);
        }

        return mapToTripResponse(savedTrip, deliveryInfo);
    }

    // 2. Lấy chi tiết chuyến xe
    @Override
    public TripResponse getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin chuyến xe!"));
        DeliveryInfo deliveryInfo = deliveryInfoRepository.findByTripId(tripId).orElse(null);
        return mapToTripResponse(trip, deliveryInfo);
    }

    // 3. Lịch sử chuyến xe khách hàng
    @Override
    public List<TripResponse> getCustomerTripHistory(Long customerId) {
        return tripRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(trip -> {
                    DeliveryInfo deliveryInfo = deliveryInfoRepository.findByTripId(trip.getId()).orElse(null);
                    return mapToTripResponse(trip, deliveryInfo);
                })
                .collect(Collectors.toList());
    }

    // 4. Cập nhật trạng thái tài xế (ONLINE/OFFLINE)
    @Override
    @Transactional
    public UserResponse updateDriverStatus(UpdateDriverStatusRequest request) {
        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Tài xế không tồn tại!"));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new RuntimeException("Tài khoản này không phải là Tài xế!");
        }

        driver.setDriverStatus(request.getDriverStatus());
        if (request.getCurrentLat() != null && request.getCurrentLng() != null) {
            driver.setCurrentLat(request.getCurrentLat());
            driver.setCurrentLng(request.getCurrentLng());
        }

        User updatedDriver = userRepository.save(driver);

        return UserResponse.builder()
                .id(updatedDriver.getId())
                .phoneNumber(updatedDriver.getPhoneNumber())
                .fullName(updatedDriver.getFullName())
                .email(updatedDriver.getEmail())
                .role(updatedDriver.getRole())
                .driverStatus(updatedDriver.getDriverStatus())
                .currentLat(updatedDriver.getCurrentLat())
                .currentLng(updatedDriver.getCurrentLng())
                .createdAt(updatedDriver.getCreatedAt())
                .build();
    }

    // 5. Tài xế nhận chuyến
    @Override
    @Transactional
    public TripResponse acceptTrip(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Chuyến xe không tồn tại!"));

        if (trip.getStatus() != TripStatus.CREATED && trip.getStatus() != TripStatus.SEARCHING) {
            throw new RuntimeException("Chuyến xe này đã được tài xế khác nhận hoặc đã bị hủy!");
        }

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Tài xế không tồn tại!"));

        if (driver.getDriverStatus() == DriverStatus.BUSY) {
            throw new RuntimeException("Tài xế đang thực hiện chuyến xe khác, không thể nhận thêm!");
        }

        trip.setDriverId(driverId);
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(LocalDateTime.now());
        Trip savedTrip = tripRepository.save(trip);

        driver.setDriverStatus(DriverStatus.BUSY);
        userRepository.save(driver);

        DeliveryInfo deliveryInfo = deliveryInfoRepository.findByTripId(tripId).orElse(null);
        return mapToTripResponse(savedTrip, deliveryInfo);
    }

    // 6. Cập nhật trạng thái chuyến xe
    @Override
    @Transactional
    public TripResponse updateTripStatus(Long tripId, UpdateTripStatusRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Chuyến xe không tồn tại!"));

        if (trip.getDriverId() == null || !trip.getDriverId().equals(request.getDriverId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật chuyến xe của tài xế khác!");
        }

        TripStatus newStatus = request.getStatus();

        if (newStatus == TripStatus.ARRIVED_PICKUP) {
            trip.setStatus(TripStatus.ARRIVED_PICKUP);
        } else if (newStatus == TripStatus.IN_PROGRESS) {
            trip.setStatus(TripStatus.IN_PROGRESS);
            trip.setStartedAt(LocalDateTime.now());
        } else if (newStatus == TripStatus.COMPLETED) {
            trip.setStatus(TripStatus.COMPLETED);
            trip.setCompletedAt(LocalDateTime.now());

            walletService.deductCommission(trip.getDriverId(), trip.getId(), trip.getPrice());

            User driver = userRepository.findById(request.getDriverId()).orElse(null);
            if (driver != null) {
                driver.setDriverStatus(DriverStatus.ONLINE);
                userRepository.save(driver);
            }
        } else if (newStatus == TripStatus.CANCELLED) {
            trip.setStatus(TripStatus.CANCELLED);
            trip.setCancelledAt(LocalDateTime.now());

            User driver = userRepository.findById(request.getDriverId()).orElse(null);
            if (driver != null) {
                driver.setDriverStatus(DriverStatus.ONLINE);
                userRepository.save(driver);
            }
        }

        Trip savedTrip = tripRepository.save(trip);
        DeliveryInfo deliveryInfo = deliveryInfoRepository.findByTripId(tripId).orElse(null);
        return mapToTripResponse(savedTrip, deliveryInfo);
    }

    // --- HÀM BỔ TRỢ 1: Tính khoảng cách Haversine ---
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    // --- HÀM BỔ TRỢ 2: Tính cước phí ---
    private BigDecimal calculatePrice(ServiceType serviceType, BigDecimal distanceKm, double surgeMultiplier) {
        double basePrice;
        double pricePerKm;

        if (serviceType == ServiceType.RIDE) {
            basePrice = 15000.0;
            pricePerKm = 6000.0;
        } else {
            basePrice = 20000.0;
            pricePerKm = 7000.0;
        }

        double total;
        double dist = distanceKm.doubleValue();
        if (dist <= 2.0) {
            total = basePrice;
        } else {
            total = basePrice + (dist - 2.0) * pricePerKm;
        }

        total = total * surgeMultiplier;
        return BigDecimal.valueOf(Math.round(total / 1000.0) * 1000.0);
    }

    // --- HÀM BỔ TRỢ 3: Map Entity sang DTO ---
    private TripResponse mapToTripResponse(Trip trip, DeliveryInfo deliveryInfo) {
        TripResponse.DeliveryInfoResponse deliveryResponse = null;
        if (deliveryInfo != null) {
            deliveryResponse = TripResponse.DeliveryInfoResponse.builder()
                    .senderName(deliveryInfo.getSenderName())
                    .senderPhone(deliveryInfo.getSenderPhone())
                    .receiverName(deliveryInfo.getReceiverName())
                    .receiverPhone(deliveryInfo.getReceiverPhone())
                    .packageName(deliveryInfo.getPackageName())
                    .weightKg(deliveryInfo.getWeightKg())
                    .noteForDriver(deliveryInfo.getNoteForDriver())
                    .build();
        }

        return TripResponse.builder()
                .id(trip.getId())
                .customerId(trip.getCustomerId())
                .driverId(trip.getDriverId())
                .serviceType(trip.getServiceType())
                .pickupLat(trip.getPickupLat())
                .pickupLng(trip.getPickupLng())
                .pickupAddress(trip.getPickupAddress())
                .dropoffLat(trip.getDropoffLat())
                .dropoffLng(trip.getDropoffLng())
                .dropoffAddress(trip.getDropoffAddress())
                .distanceKm(trip.getDistanceKm())
                .price(trip.getPrice())
                .surgeMultiplier(trip.getSurgeMultiplier())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .deliveryInfo(deliveryResponse)
                .build();
    }
}