package org.example.ridehailing.walletservice.service;

import org.example.ridehailing.walletservice.dto.request.DeductCommissionRequest;
import org.example.ridehailing.walletservice.dto.request.TopUpWalletRequest;
import org.example.ridehailing.walletservice.entity.Wallet;

import java.math.BigDecimal;

public interface WalletService {
    Wallet getWalletByUserId(Long userId);
    Wallet topUpWallet(TopUpWalletRequest request);
    void deductCommission(DeductCommissionRequest request);
    BigDecimal getBalance(Long userId);
}