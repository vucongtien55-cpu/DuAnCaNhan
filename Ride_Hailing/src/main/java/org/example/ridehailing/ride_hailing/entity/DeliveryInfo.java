package org.example.ridehailing.ride_hailing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_infos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tripId; // Đơn giao hàng thuộc về Trip nào

    @Column(nullable = false, length = 100)
    private String senderName; // Tên người gửi

    @Column(nullable = false, length = 15)
    private String senderPhone; // SĐT người gửi

    @Column(nullable = false, length = 100)
    private String receiverName; // Tên người nhận

    @Column(nullable = false, length = 15)
    private String receiverPhone; // SĐT người nhận

    @Column(length = 255)
    private String packageName; // Tên kiện hàng

    private Double weightKg; // Trọng lượng (kg)

    @Column(length = 255)
    private String noteForDriver; // Ghi chú cho tài xế
}