//package org.example.ridehailing.ride_hailing.controller;
//
//import org.example.ridehailing.ride_hailing.dto.request.TopUpWalletRequest;
//import org.example.ridehailing.ride_hailing.dto.response.WalletResponse;
//import org.example.ridehailing.ride_hailing.service.WalletService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/wallets")
//@RequiredArgsConstructor
//public class WalletController {
//
//    private final WalletService walletService;
//
//    // API 1: Xem số dư ví của Tài xế
//    @GetMapping("/driver/{driverId}")
//    public ResponseEntity<WalletResponse> getWalletByDriverId(@PathVariable("driverId") Long driverId) {
//        WalletResponse response = walletService.getWalletByDriverId(driverId);
//        return ResponseEntity.ok(response);
//    }
//
//    // API 2: Nạp tiền vào Ví tài xế
//    @PostMapping("/topup")
//    public ResponseEntity<WalletResponse> topUpWallet(@Valid @RequestBody TopUpWalletRequest request) {
//        WalletResponse response = walletService.topUpWallet(request);
//        return ResponseEntity.ok(response);
//    }
//}