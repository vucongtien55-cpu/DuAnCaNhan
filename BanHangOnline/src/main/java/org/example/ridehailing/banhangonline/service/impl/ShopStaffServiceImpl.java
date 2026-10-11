package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.example.ridehailing.banhangonline.service.ShopStaffService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShopStaffServiceImpl implements ShopStaffService {

    private final ShopRepository shopRepository;
    private final ShopStaffRepository shopStaffRepository;
    private final UserRepository userRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public ShopStaffServiceImpl(
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
    public List<ShopStaff> getActiveStaff(Long actorUserId, Long shopId) {
        requirePositiveId(actorUserId, "actorUserId");
        Shop shop = getShop(shopId);
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return shopStaffRepository.findByShop_IdAndActiveTrue(shopId);
    }

    @Override
    public List<ShopStaff> getStaff(Long actorUserId, Long shopId, Boolean active) {
        requirePositiveId(actorUserId, "actorUserId");
        Shop shop = getShop(shopId);
        if (Boolean.FALSE.equals(active)) {
            shopAuthorizationService.requireShopOwner(actorUserId, shop);
            return shopStaffRepository.findByShop_IdAndActiveFalse(shopId);
        }
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return shopStaffRepository.findByShop_IdAndActiveTrue(shopId);
    }

    @Override
    @Transactional
    public ShopStaff addStaff(Long actorUserId, Long shopId, Long userId) {
        requirePositiveId(actorUserId, "actorUserId");
        requirePositiveId(userId, "userId");
        Shop shop = getShop(shopId);
        shopAuthorizationService.requireShopOwner(actorUserId, shop);

        User user = userRepository.findById(userId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng đang hoạt động."));
        if (shop.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Chủ cửa hàng không thể được thêm làm nhân viên của chính mình.");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("Không thể thêm tài khoản quản trị làm nhân viên cửa hàng.");
        }

        return shopStaffRepository.findByShop_IdAndUser_Id(shopId, userId)
                .map(existing -> reactivateStaff(existing, shop, user))
                .orElseGet(() -> createStaff(shop, user));
    }

    @Override
    @Transactional
    public void deactivateStaff(Long actorUserId, Long shopId, Long staffId) {
        requirePositiveId(actorUserId, "actorUserId");
        requirePositiveId(staffId, "staffId");
        Shop shop = getShop(shopId);
        shopAuthorizationService.requireShopOwner(actorUserId, shop);

        ShopStaff staff = shopStaffRepository.findById(staffId)
                .filter(existing -> existing.getShop().getId().equals(shopId))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên thuộc cửa hàng này."));
        staff.setActive(false);
        shopStaffRepository.save(staff);
        updateGlobalRoleAfterDeactivation(staff.getUser());
    }

    private ShopStaff reactivateStaff(ShopStaff staff, Shop shop, User user) {
        if (Boolean.TRUE.equals(staff.getActive())) {
            throw new IllegalArgumentException("Người dùng đã là nhân viên đang hoạt động của cửa hàng.");
        }
        staff.setActive(true);
        staff.setShop(shop);
        promoteToEmployee(user);
        userRepository.save(user);
        return shopStaffRepository.save(staff);
    }

    private ShopStaff createStaff(Shop shop, User user) {
        ShopStaff staff = new ShopStaff();
        staff.setShop(shop);
        staff.setUser(user);
        staff.setRole("STAFF");
        staff.setActive(true);
        promoteToEmployee(user);
        userRepository.save(user);
        return shopStaffRepository.save(staff);
    }

    // Vai trò toàn cục là một giá trị; tài khoản quản trị/chủ shop được giữ nguyên vai trò cao hơn.
    private void promoteToEmployee(User user) {
        if ("CUSTOMER".equals(user.getRole())) {
            user.setRole("EMPLOYEE");
        }
    }

    // Chỉ hạ vai trò toàn cục khi tài khoản không còn nhân viên hay chủ shop hoạt động nào.
    private void updateGlobalRoleAfterDeactivation(User user) {
        if (!"EMPLOYEE".equals(user.getRole())
                || shopStaffRepository.existsByUser_IdAndActiveTrue(user.getId())
                || shopRepository.existsByOwner_IdAndStatus(user.getId(), "ACTIVE")) {
            return;
        }
        user.setRole("CUSTOMER");
        userRepository.save(user);
    }

    private Shop getShop(Long shopId) {
        requirePositiveId(shopId, "shopId");
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
