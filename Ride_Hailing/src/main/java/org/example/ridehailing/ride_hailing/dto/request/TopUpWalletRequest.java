package org.example.ridehailing.ride_hailing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TopUpWalletRequest {

    @NotNull(message = "ID tài xế không được để trống")
    private Long driverId;

    @NotNull(message = "Số tiền nạp không được để trống")
    @Min(value = 10000, message = "Số tiền nạp tối thiểu là 10.000 VNĐ")
    private BigDecimal amount;
}