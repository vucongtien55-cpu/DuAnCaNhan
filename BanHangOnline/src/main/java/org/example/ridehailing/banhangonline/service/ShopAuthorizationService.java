package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Shop;

public interface ShopAuthorizationService {

    // Bảo đảm người thực hiện là chủ shop hoặc nhân viên còn hoạt động của shop.
    void requireShopMember(Long actorUserId, Shop shop);

    // Bảo đảm người thực hiện là chủ sở hữu shop.
    void requireShopOwner(Long actorUserId, Shop shop);
}
