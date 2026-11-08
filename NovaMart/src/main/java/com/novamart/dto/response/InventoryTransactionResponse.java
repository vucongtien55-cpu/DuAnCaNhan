package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {
    private Long id;
    private Long variantId;
    private String sku;
    private int changeQuantity;
    private String transactionType;
    private String referenceType;
    private Long referenceId;
    private String note;
    private String createdByEmail;
    private String createdByName;
    private LocalDateTime createdAt;
}

