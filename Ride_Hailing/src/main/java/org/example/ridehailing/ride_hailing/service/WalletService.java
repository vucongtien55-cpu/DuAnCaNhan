package org.example.ridehailing.ride_hailing.service;

import org.example.ridehailing.ride_hailing.dto.request.TopUpWalletRequest;
import org.example.ridehailing.ride_hailing.dto.response.WalletResponse;

import java.math.BigDecimal;

public interface WalletService {
    // 1. Lấy thông tin Ví tiền theo Driver ID
    WalletResponse getWalletByDriverId(Long driverId);

    // 2. Nạp tiền vào Ví
    WalletResponse topUpWallet(TopUpWalletRequest request);

    // 3. Trừ tiền hoa hồng khi hoàn thành chuyến xe (20% giá trị chuyến)
    void deductCommission(Long driverId, Long tripId, BigDecimal tripPrice);
}