package com.novamart.controller;

import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.InventoryResponse;
import com.novamart.dto.response.InventoryTransactionResponse;
import com.novamart.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/inventory")
@PreAuthorize("hasAuthority('ROLE_SELLER')")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Xem tồn kho hiện tại của một biến thể
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long variantId) {
        InventoryResponse inventory = inventoryService.getInventoryResponseByVariantId(variantId);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    // Seller nhập thêm hàng hoặc điều chỉnh kho thủ công (kiểm tra quyền sở hữu Shop)
    @PostMapping("/variants/{variantId}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(Authentication authentication,
                                                                      @PathVariable Long variantId,
                                                                      @RequestParam int changeQuantity,
                                                                      @RequestParam(defaultValue = "Điều chỉnh kho thủ công") String note) {
        InventoryResponse inventory = inventoryService.adjustStock(authentication.getName(), variantId, changeQuantity, note);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", inventory));
    }

    // Xem lịch sử biến động kho của biến thể (kiểm tra quyền sở hữu Shop)
    @GetMapping("/variants/{variantId}/history")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> getStockHistory(Authentication authentication,
                                                                                           @PathVariable Long variantId) {
        List<InventoryTransactionResponse> history = inventoryService.getStockHistory(authentication.getName(), variantId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
