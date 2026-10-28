package org.example.ridehailing.walletservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpWalletRequest {

    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotNull(message = "amount không được để trống")
    @DecimalMin(value = "10000.0", message = "Số tiền nạp tối thiểu là 10,000 VNĐ")
    private BigDecimal amount;
}