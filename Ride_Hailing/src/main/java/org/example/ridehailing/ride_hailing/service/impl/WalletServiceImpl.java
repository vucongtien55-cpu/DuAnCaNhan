package org.example.ridehailing.ride_hailing.service.impl;

import org.example.ridehailing.ride_hailing.dto.request.TopUpWalletRequest;
import org.example.ridehailing.ride_hailing.dto.response.WalletResponse;
import org.example.ridehailing.ride_hailing.entity.TransactionType;
import org.example.ridehailing.ride_hailing.entity.Wallet;
import org.example.ridehailing.ride_hailing.entity.WalletTransaction;
import org.example.ridehailing.ride_hailing.repository.WalletRepository;
import org.example.ridehailing.ride_hailing.repository.WalletTransactionRepository;
import org.example.ridehailing.ride_hailing.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // Tỷ lệ hoa hồng hệ thống thu từ tài xế (20%)
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.20");

    @Override
    public WalletResponse getWalletByDriverId(Long driverId) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Ví tiền cho tài xế này!"));
        return mapToWalletResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse topUpWallet(TopUpWalletRequest request) {
        Wallet wallet = walletRepository.findByDriverId(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Ví tiền không tồn tại!"));

        // Cộng số dư
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);

        // Lưu lịch sử giao dịch nạp tiền
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(savedWallet.getId())
                .driverId(request.getDriverId())
                .amount(request.getAmount())
                .transactionType(TransactionType.TOP_UP)
                .description("Nạp tiền vào tài khoản ví")
                .build();
        walletTransactionRepository.save(transaction);

        return mapToWalletResponse(savedWallet);
    }

    @Override
    @Transactional
    public void deductCommission(Long driverId, Long tripId, BigDecimal tripPrice) {
        Wallet wallet = walletRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Ví tiền tài xế không tồn tại!"));

        // Tính tiền hoa hồng = Giá chuyến xe * 20%
        BigDecimal commissionAmount = tripPrice.multiply(COMMISSION_RATE);

        // Trừ tiền trong ví
        wallet.setBalance(wallet.getBalance().subtract(commissionAmount));
        walletRepository.save(wallet);

        // Lưu lịch sử giao dịch trừ hoa hồng
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .driverId(driverId)
                .tripId(tripId)
                .amount(commissionAmount)
                .transactionType(TransactionType.COMMISSION_DEDUCTION)
                .description("Trích trừ 20% hoa hồng cho chuyến xe #" + tripId)
                .build();
        walletTransactionRepository.save(transaction);
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .driverId(wallet.getDriverId())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}