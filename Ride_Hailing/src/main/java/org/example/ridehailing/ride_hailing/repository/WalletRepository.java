package org.example.ridehailing.ride_hailing.repository;

import org.example.ridehailing.ride_hailing.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Tìm ví tiền theo ID của tài xế
    Optional<Wallet> findByDriverId(Long driverId);
}