package org.example.ridehailing.walletservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeductCommissionRequest {
    @NotNull
    private Long driverId;

    @NotNull
    private BigDecimal amount;

    private String description;
}
