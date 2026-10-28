package org.example.ridehailing.walletservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.walletservice.dto.request.DeductCommissionRequest;
import org.example.ridehailing.walletservice.dto.request.TopUpWalletRequest;
import org.example.ridehailing.walletservice.entity.Wallet;
import org.example.ridehailing.walletservice.entity.WalletTransaction;
import org.example.ridehailing.walletservice.repository.WalletRepository;
import org.example.ridehailing.walletservice.repository.WalletTransactionRepository;
import org.example.ridehailing.walletservice.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }

    @Override
    @Transactional
    public Wallet topUpWallet(TopUpWalletRequest request) {
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .userId(request.getUserId())
                                .balance(BigDecimal.ZERO)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));

        BigDecimal currentBalance = (wallet.getBalance() != null) ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(currentBalance.add(request.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());
        Wallet savedWallet = walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(savedWallet.getId())
                .amount(request.getAmount())
                .type(WalletTransaction.TransactionType.TOP_UP)
                .description("Nạp tiền vào ví qua API Gateway")
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        return savedWallet;
    }

    @Override
    @Transactional
    public void deductCommission(DeductCommissionRequest request) {
        Wallet wallet = walletRepository.findByUserId(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại cho tài xế ID: " + request.getDriverId()));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Số dư ví không đủ để trừ hoa hồng!");
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .amount(request.getAmount())
                .type(WalletTransaction.TransactionType.DEDUCT)
                .description(request.getDescription() != null ? request.getDescription() : "Trừ hoa hồng chuyến xe")
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        return getWalletByUserId(userId).getBalance();
    }
}