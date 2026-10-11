package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ShopAuthorizationServiceImpl implements ShopAuthorizationService {

    private final ShopStaffRepository shopStaffRepository;

    public ShopAuthorizationServiceImpl(ShopStaffRepository shopStaffRepository) {
        this.shopStaffRepository = shopStaffRepository;
    }

    @Override
    public void requireShopMember(Long actorUserId, Shop shop) {
        validateActorAndShop(actorUserId, shop);
        if (shop.getOwner() != null && actorUserId.equals(shop.getOwner().getId())) {
            return;
        }

        ShopStaff staff = shopStaffRepository.findByShop_IdAndUser_Id(shop.getId(), actorUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền truy cập cửa hàng này."));
        if (!Boolean.TRUE.equals(staff.getActive())) {
            throw new AccessDeniedException("Tư cách nhân viên tại cửa hàng này không còn hoạt động.");
        }
    }

    @Override
    public void requireShopOwner(Long actorUserId, Shop shop) {
        validateActorAndShop(actorUserId, shop);
        if (shop.getOwner() == null || !actorUserId.equals(shop.getOwner().getId())) {
            throw new AccessDeniedException("Chỉ chủ sở hữu mới được thực hiện thao tác này.");
        }
    }

    private void validateActorAndShop(Long actorUserId, Shop shop) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId phải là số dương.");
        }
        if (shop == null || shop.getId() == null) {
            throw new EntityNotFoundException("Không tìm thấy cửa hàng.");
        }
    }
}
