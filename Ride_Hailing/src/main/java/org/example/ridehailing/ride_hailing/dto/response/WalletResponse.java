package org.example.ridehailing.ride_hailing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class WalletResponse {

    private Long id;
    private Long driverId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}