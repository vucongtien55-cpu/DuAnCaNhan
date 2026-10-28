package org.example.ridehailing.walletservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.walletservice.dto.request.DeductCommissionRequest;
import org.example.ridehailing.walletservice.dto.request.TopUpWalletRequest;
import org.example.ridehailing.walletservice.entity.Wallet;
import org.example.ridehailing.walletservice.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/topup")
    public ResponseEntity<Wallet> topUp(@Valid @RequestBody TopUpWalletRequest request) {
        return ResponseEntity.ok(walletService.topUpWallet(request));
    }

    @PostMapping("/deduct")
    public ResponseEntity<String> deductCommission(@Valid @RequestBody DeductCommissionRequest request) {
        walletService.deductCommission(request);
        return ResponseEntity.ok("Trừ hoa hồng thành công!");
    }
}