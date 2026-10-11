package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.entity.ShopTransaction;
import org.example.ridehailing.banhangonline.exception.DuplicateResourceException;
import org.example.ridehailing.banhangonline.repository.ShopOrderRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopTransactionRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.example.ridehailing.banhangonline.service.ShopTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ShopTransactionServiceImpl implements ShopTransactionService {

    private static final Set<String> TRANSACTION_TYPES = Set.of("SALE", "REFUND", "PAYOUT", "FEE", "ADJUSTMENT");

    private final ShopTransactionRepository shopTransactionRepository;
    private final ShopRepository shopRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public ShopTransactionServiceImpl(
            ShopTransactionRepository shopTransactionRepository,
            ShopRepository shopRepository,
            ShopOrderRepository shopOrderRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.shopTransactionRepository = shopTransactionRepository;
        this.shopRepository = shopRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public List<ShopTransaction> getShopTransactions(Long actorUserId, Long shopId) {
        authorizeShop(actorUserId, shopId);
        return shopTransactionRepository.findByShop_IdOrderByCreatedAtDesc(shopId);
    }

    @Override
    public List<ShopTransaction> getShopTransactions(Long actorUserId, Long shopId, String type) {
        authorizeShop(actorUserId, shopId);
        if (type == null || type.isBlank()) {
            return shopTransactionRepository.findByShop_IdOrderByCreatedAtDesc(shopId);
        }
        return shopTransactionRepository.findByShop_IdAndTypeOrderByCreatedAtDesc(
                shopId, normalizeType(type));
    }

    @Override
    public List<ShopTransaction> getShopOrderTransactions(Long actorUserId, Long shopId, Long shopOrderId) {
        authorizeShop(actorUserId, shopId);
        requirePositiveId(shopOrderId, "shopOrderId");
        shopOrderRepository.findByIdAndShop_Id(shopOrderId, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn thuộc cửa hàng này."));
        return shopTransactionRepository.findByShopOrder_IdOrderByCreatedAtDesc(shopOrderId);
    }

    @Override
    @Transactional
    public ShopTransaction createPendingTransaction(
            Long actorUserId, Long shopId, Long shopOrderId, String type, BigDecimal amount,
            String transactionReference) {
        Shop shop = getShop(shopId);
        shopAuthorizationService.requireShopOwner(actorUserId, shop);
        String normalizedType = normalizeType(type);
        BigDecimal normalizedAmount = normalizeAmount(amount);

        ShopOrder shopOrder = null;
        if (shopOrderId != null) {
            requirePositiveId(shopOrderId, "shopOrderId");
            shopOrder = shopOrderRepository.findByIdAndShop_Id(shopOrderId, shopId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn thuộc cửa hàng này."));
        }

        String normalizedReference = transactionReference == null || transactionReference.isBlank()
                ? null : transactionReference.trim();
        if (normalizedReference != null) {
            if (normalizedReference.length() > 120) {
                throw new IllegalArgumentException("Mã giao dịch không được vượt quá 120 ký tự.");
            }
            if (shopTransactionRepository.findByTransactionReference(normalizedReference).isPresent()) {
                throw new DuplicateResourceException("Mã giao dịch đã tồn tại.");
            }
        }

        ShopTransaction transaction = new ShopTransaction();
        transaction.setShop(shop);
        transaction.setShopOrder(shopOrder);
        transaction.setType(normalizedType);
        transaction.setAmount(normalizedAmount);
        transaction.setTransactionReference(normalizedReference);
        transaction.setStatus("PENDING");
        return shopTransactionRepository.save(transaction);
    }

    private Shop authorizeShop(Long actorUserId, Long shopId) {
        Shop shop = getShop(shopId);
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return shop;
    }

    private Shop getShop(Long shopId) {
        requirePositiveId(shopId, "shopId");
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
    }

    private String normalizeType(String type) {
        if (type == null || !TRANSACTION_TYPES.contains(type.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Loại giao dịch không được hỗ trợ.");
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền giao dịch phải lớn hơn 0.");
        }
        try {
            BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.precision() > 12) {
                throw new IllegalArgumentException("Số tiền vượt quá giới hạn 12 chữ số.");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Số tiền chỉ được có tối đa 2 chữ số thập phân.", exception);
        }
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
