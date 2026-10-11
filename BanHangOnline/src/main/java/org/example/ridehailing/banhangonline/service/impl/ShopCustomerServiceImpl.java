package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopCustomer;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.ShopCustomerRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.example.ridehailing.banhangonline.service.ShopCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ShopCustomerServiceImpl implements ShopCustomerService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "BLOCKED");

    private final ShopCustomerRepository shopCustomerRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public ShopCustomerServiceImpl(
            ShopCustomerRepository shopCustomerRepository,
            ShopRepository shopRepository,
            UserRepository userRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.shopCustomerRepository = shopCustomerRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public List<ShopCustomer> getCustomers(Long actorUserId, Long shopId) {
        Shop shop = getAuthorizedShop(actorUserId, shopId);
        return shopCustomerRepository.findByShop_Id(shop.getId());
    }

    @Override
    public List<ShopCustomer> getCustomers(Long actorUserId, Long shopId, String status) {
        Shop shop = getAuthorizedShop(actorUserId, shopId);
        if (status == null || status.isBlank()) {
            return shopCustomerRepository.findByShop_Id(shop.getId());
        }
        return shopCustomerRepository.findByShop_IdAndStatus(shop.getId(), normalizeStatus(status));
    }

    @Override
    @Transactional
    public ShopCustomer addCustomer(Long actorUserId, Long shopId, Long customerId) {
        Shop shop = getAuthorizedShop(actorUserId, shopId);
        requirePositiveId(customerId, "customerId");

        User customer = userRepository.findById(customerId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng đang hoạt động."));
        return shopCustomerRepository.findByShop_IdAndCustomer_Id(shopId, customerId)
                .map(existing -> reactivate(existing))
                .orElseGet(() -> createMembership(shop, customer));
    }

    @Override
    @Transactional
    public ShopCustomer updateCustomerStatus(
            Long actorUserId, Long shopId, Long customerId, String status) {
        getAuthorizedShop(actorUserId, shopId);
        requirePositiveId(customerId, "customerId");
        String normalizedStatus = normalizeStatus(status);
        ShopCustomer membership = shopCustomerRepository.findByShop_IdAndCustomer_Id(shopId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Khách hàng chưa thuộc danh sách của cửa hàng."));
        membership.setStatus(normalizedStatus);
        return shopCustomerRepository.save(membership);
    }

    private ShopCustomer reactivate(ShopCustomer membership) {
        if ("ACTIVE".equals(membership.getStatus())) {
            throw new IllegalArgumentException("Khách hàng đã có trạng thái hoạt động tại cửa hàng.");
        }
        membership.setStatus("ACTIVE");
        return shopCustomerRepository.save(membership);
    }

    private ShopCustomer createMembership(Shop shop, User customer) {
        ShopCustomer membership = new ShopCustomer();
        membership.setShop(shop);
        membership.setCustomer(customer);
        membership.setStatus("ACTIVE");
        return shopCustomerRepository.save(membership);
    }

    private Shop getAuthorizedShop(Long actorUserId, Long shopId) {
        requirePositiveId(shopId, "shopId");
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return shop;
    }

    private String normalizeStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status.trim().toUpperCase())) {
            throw new IllegalArgumentException("Trạng thái khách hàng phải là ACTIVE, INACTIVE hoặc BLOCKED.");
        }
        return status.trim().toUpperCase();
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
