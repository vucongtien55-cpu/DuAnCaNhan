package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.ShopCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopCustomerRepository extends JpaRepository<ShopCustomer, Long> {

    // Lấy danh sách khách hàng của cửa hàng.
    List<ShopCustomer> findByShop_Id(Long shopId);

    // Lọc khách hàng của cửa hàng theo trạng thái.
    List<ShopCustomer> findByShop_IdAndStatus(Long shopId, String status);

    // Tìm bản ghi liên kết khách hàng với một cửa hàng.
    Optional<ShopCustomer> findByShop_IdAndCustomer_Id(Long shopId, Long customerId);

}
