package org.example.ridehailing.ride_hailing.repository;

import org.example.ridehailing.ride_hailing.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Lấy lịch sử giao dịch ví của tài xế
    List<WalletTransaction> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}