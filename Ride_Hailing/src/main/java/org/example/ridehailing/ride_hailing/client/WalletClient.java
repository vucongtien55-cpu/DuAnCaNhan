package org.example.ridehailing.ride_hailing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "WALLET-SERVICE")
public interface WalletClient {

    @PostMapping("/api/v1/wallets/deduct")
    ResponseEntity<String> deductCommission(@RequestBody DeductCommissionRequest request);

    class DeductCommissionRequest {
        private Long driverId;
        private BigDecimal amount;
        private String description;

        public DeductCommissionRequest() {}

        public DeductCommissionRequest(Long driverId, BigDecimal amount, String description) {
            this.driverId = driverId;
            this.amount = amount;
            this.description = description;
        }

        public Long getDriverId() { return driverId; }
        public void setDriverId(Long driverId) { this.driverId = driverId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}