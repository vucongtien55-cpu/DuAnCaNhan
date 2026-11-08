package com.novamart.service.impl;

import com.novamart.dto.response.InventoryResponse;
import com.novamart.dto.response.InventoryTransactionResponse;
import com.novamart.entity.Inventory;
import com.novamart.entity.InventoryTransaction;
import com.novamart.entity.User;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.InventoryRepository;
import com.novamart.repository.InventoryTransactionRepository;
import com.novamart.repository.UserRepository;
import com.novamart.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Inventory getInventoryByVariantId(Long variantId) {
        return inventoryRepository.findByVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin kho của biến thể"));
    }

    @Override
    public InventoryResponse getInventoryResponseByVariantId(Long variantId) {
        Inventory inventory = getInventoryByVariantId(variantId);
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(String sellerEmail, Long variantId, int changeQuantity, String note) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Dùng Lock bi quan để tránh race condition khi cập nhật kho
        Inventory inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin kho của biến thể"));

        // Kiểm tra quyền sở hữu: Biến thể phải thuộc về gian hàng của Seller đang đăng nhập
        Long shopOwnerId = inventory.getVariant().getProduct().getShop().getOwner().getId();
        if (!shopOwnerId.equals(seller.getId())) {
            throw new BadRequestException("Bạn không có quyền điều chỉnh kho của sản phẩm không thuộc gian hàng của mình!");
        }

        int newQuantity = inventory.getQuantityAvailable() + changeQuantity;
        if (newQuantity < 0) {
            throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn 0 sau khi điều chỉnh!");
        }

        inventory.setQuantityAvailable(newQuantity);
        Inventory savedInv = inventoryRepository.save(inventory);

        // Lưu vết lịch sử biến động kho (Audit trail)
        InventoryTransaction tx = InventoryTransaction.builder()
                .variant(inventory.getVariant())
                .changeQuantity(changeQuantity)
                .transactionType(changeQuantity > 0 ? "RESTOCK" : "ADJUST")
                .note(note)
                .createdBy(seller)
                .build();
        transactionRepository.save(tx);

        return mapToInventoryResponse(savedInv);
    }

    @Override
    @Transactional
    public void reserveStock(Long variantId, int quantity, Long orderId) {
        // Dùng Lock bi quan ngăn chặn bán âm kho khi nhiều khách đặt cùng lúc
        Inventory inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin kho của biến thể ID: " + variantId));

        if (inventory.getQuantityAvailable() < quantity) {
            throw new BadRequestException("Sản phẩm SKU " + inventory.getVariant().getSku() + " đã hết hàng hoặc không đủ số lượng!");
        }

        // Trừ số lượng sẵn bán và chuyển sang giữ chỗ
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);
        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventoryRepository.save(inventory);

        InventoryTransaction tx = InventoryTransaction.builder()
                .variant(inventory.getVariant())
                .changeQuantity(-quantity)
                .transactionType("RESERVE")
                .referenceType("ORDER")
                .referenceId(orderId)
                .note("Giữ chỗ tồn kho cho đơn hàng #" + orderId)
                .build();
        transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void releaseStock(Long variantId, int quantity, Long orderId) {
        Inventory inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin kho của biến thể ID: " + variantId));

        inventory.setQuantityReserved(Math.max(0, inventory.getQuantityReserved() - quantity));
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
        inventoryRepository.save(inventory);

        InventoryTransaction tx = InventoryTransaction.builder()
                .variant(inventory.getVariant())
                .changeQuantity(quantity)
                .transactionType("RELEASE")
                .referenceType("ORDER")
                .referenceId(orderId)
                .note("Hoàn tồn kho từ đơn hàng bị hủy #" + orderId)
                .build();
        transactionRepository.save(tx);
    }

    @Override
    public List<InventoryTransactionResponse> getStockHistory(String sellerEmail, Long variantId) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Inventory inventory = getInventoryByVariantId(variantId);
        Long shopOwnerId = inventory.getVariant().getProduct().getShop().getOwner().getId();
        if (!shopOwnerId.equals(seller.getId())) {
            throw new BadRequestException("Bạn không có quyền xem lịch sử kho của sản phẩm không thuộc gian hàng của mình!");
        }

        return transactionRepository.findByVariantIdOrderByCreatedAtDesc(variantId).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .variantId(inventory.getVariant().getId())
                .sku(inventory.getVariant().getSku())
                .quantityAvailable(inventory.getQuantityAvailable())
                .quantityReserved(inventory.getQuantityReserved())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private InventoryTransactionResponse mapToTransactionResponse(InventoryTransaction tx) {
        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .variantId(tx.getVariant().getId())
                .sku(tx.getVariant().getSku())
                .changeQuantity(tx.getChangeQuantity())
                .transactionType(tx.getTransactionType())
                .referenceType(tx.getReferenceType())
                .referenceId(tx.getReferenceId())
                .note(tx.getNote())
                .createdByEmail(tx.getCreatedBy() != null ? tx.getCreatedBy().getEmail() : null)
                .createdByName(tx.getCreatedBy() != null ? tx.getCreatedBy().getFullName() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
