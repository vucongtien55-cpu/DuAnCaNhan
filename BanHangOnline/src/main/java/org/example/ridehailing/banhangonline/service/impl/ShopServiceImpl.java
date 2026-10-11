package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.example.ridehailing.banhangonline.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShopServiceImpl implements ShopService {

    private static final java.util.Set<String> SHOP_STATUSES = java.util.Set.of("ACTIVE", "INACTIVE");

    private static final int NAME_MAX_LENGTH = 150;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int ADDRESS_MAX_LENGTH = 300;
    private static final int PHONE_MAX_LENGTH = 20;
    private static final int EMAIL_MAX_LENGTH = 180;

    private final ShopRepository shopRepository;
    private final ShopStaffRepository shopStaffRepository;
    private final UserRepository userRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public ShopServiceImpl(
            ShopRepository shopRepository,
            ShopStaffRepository shopStaffRepository,
            UserRepository userRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.shopRepository = shopRepository;
        this.shopStaffRepository = shopStaffRepository;
        this.userRepository = userRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public Shop getShopById(Long shopId) {
        requirePositiveId(shopId, "shopId");
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
    }

    @Override
    public Shop getActiveShopById(Long shopId) {
        requirePositiveId(shopId, "shopId");
        return shopRepository.findByIdAndStatus(shopId, "ACTIVE")
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng đang hoạt động."));
    }

    @Override
    public List<Shop> getShopsByOwner(Long ownerId) {
        requirePositiveId(ownerId, "ownerId");
        return shopRepository.findByOwner_Id(ownerId);
    }

    @Override
    public List<Shop> getShopsForEmployee(Long employeeId) {
        requirePositiveId(employeeId, "employeeId");
        return shopStaffRepository.findByUser_IdAndActiveTrue(employeeId).stream()
                .map(staff -> staff.getShop())
                .toList();
    }

    @Override
    public List<Shop> getShopsByStatus(Long actorUserId, String status) {
        requirePositiveId(actorUserId, "actorUserId");
        User actor = userRepository.findById(actorUserId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy quản trị viên đang hoạt động."));
        if (!"ADMIN".equals(actor.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ quản trị viên được lọc cửa hàng toàn hệ thống.");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái cửa hàng không được để trống.");
        }
        String normalizedStatus = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!SHOP_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái cửa hàng không hợp lệ.");
        }
        return shopRepository.findByStatus(normalizedStatus);
    }

    @Override
    public List<Shop> searchActiveShops(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm cửa hàng không được để trống.");
        }
        return shopRepository.findByNameContainingIgnoreCaseAndStatus(keyword.trim(), "ACTIVE");
    }

    @Override
    @Transactional
    public Shop createShop(Long ownerId, Shop shop) {
        requirePositiveId(ownerId, "ownerId");
        validateShop(shop);
        User owner = userRepository.findById(ownerId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chủ sở hữu đang hoạt động."));
        if ("ADMIN".equals(owner.getRole())) {
            throw new IllegalArgumentException("Tài khoản quản trị không thể đăng ký cửa hàng.");
        }

        shop.setId(null);
        shop.setOwner(owner);
        shop.setName(shop.getName().trim());
        shop.setStatus("ACTIVE");
        owner.setRole("SHOP_OWNER");
        userRepository.save(owner);
        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public Shop updateShop(Long actorUserId, Long shopId, Shop updates) {
        validateShop(updates);
        Shop shop = getShopById(shopId);
        shopAuthorizationService.requireShopOwner(actorUserId, shop);

        shop.setName(updates.getName().trim());
        shop.setDescription(updates.getDescription());
        shop.setAddress(updates.getAddress());
        shop.setPhone(updates.getPhone());
        shop.setEmail(updates.getEmail());
        shop.setLogoUrl(updates.getLogoUrl());
        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public void deactivateShop(Long actorUserId, Long shopId) {
        Shop shop = getShopById(shopId);
        shopAuthorizationService.requireShopOwner(actorUserId, shop);
        shop.setStatus("INACTIVE");
        shopRepository.save(shop);
        User owner = shop.getOwner();
        if ("SHOP_OWNER".equals(owner.getRole())
                && !shopRepository.existsByOwner_IdAndStatus(owner.getId(), "ACTIVE")) {
            owner.setRole(shopStaffRepository.existsByUser_IdAndActiveTrue(owner.getId())
                    ? "EMPLOYEE" : "CUSTOMER");
            userRepository.save(owner);
        }
    }

    private void validateShop(Shop shop) {
        if (shop == null) {
            throw new IllegalArgumentException("Thông tin cửa hàng không được để trống.");
        }
        if (shop.getName() == null || shop.getName().isBlank() || shop.getName().trim().length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Tên cửa hàng phải có từ 1 đến 150 ký tự.");
        }
        if (shop.getDescription() != null && shop.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("Mô tả cửa hàng không được vượt quá 2000 ký tự.");
        }
        if (shop.getAddress() != null && shop.getAddress().length() > ADDRESS_MAX_LENGTH) {
            throw new IllegalArgumentException("Địa chỉ cửa hàng không được vượt quá 300 ký tự.");
        }
        if (shop.getPhone() != null && shop.getPhone().length() > PHONE_MAX_LENGTH) {
            throw new IllegalArgumentException("Số điện thoại cửa hàng không được vượt quá 20 ký tự.");
        }
        if (shop.getEmail() != null && shop.getEmail().length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException("Email cửa hàng không được vượt quá 180 ký tự.");
        }
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
