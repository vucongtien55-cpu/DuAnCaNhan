package org.example.ridehailing.ride_hailing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long driverId; // ID tài xế sở hữu

    @Column(nullable = false, length = 20, unique = true)
    private String licensePlate; // Biển số xe

    @Column(nullable = false, length = 50)
    private String brand; // Hãng xe (Honda, Yamaha, Toyota...)

    @Column(nullable = false, length = 50)
    private String model; // Dòng xe (AirBlade, Wave, Vios...)

    @Column(length = 30)
    private String color; // Màu xe

    @Column(nullable = false, length = 20)
    private String vehicleType; // MOTORBIKE, CAR_4_SEATS, CAR_7_SEATS
}