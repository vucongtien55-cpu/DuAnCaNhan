package com.novamart.controller;

import com.novamart.dto.request.ApplyShopRequest;
import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.ShopResponse;
import com.novamart.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * Public API: Xem thông tin gian hàng theo slug công khai
     */
    @GetMapping("/shops/{slug}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopBySlug(@PathVariable String slug) {
        ShopResponse shop = shopService.getShopBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin gian hàng thành công", shop));
    }

    /**
     * Customer API: Gửi yêu cầu đăng ký mở Gian hàng mới
     * Trạng thái khởi tạo sẽ là PENDING (chờ Admin phê duyệt).
     */
    @PostMapping("/shops/apply")
    public ResponseEntity<ApiResponse<ShopResponse>> applyShop(Authentication authentication,
                                                               @Valid @RequestBody ApplyShopRequest request) {
        ShopResponse shop = shopService.applyShop(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu mở gian hàng, vui lòng chờ Admin phê duyệt", shop));
    }

    /**
     * Seller API: Xem thông tin gian hàng cá nhân của mình
     * Yêu cầu quyền ROLE_SELLER.
     */
    @GetMapping("/seller/shops/me")
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(Authentication authentication) {
        ShopResponse shop = shopService.getMyShop(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin gian hàng thành công", shop));
    }

    /**
     * Admin API: Lấy danh sách các gian hàng theo trạng thái lọc
     * @param status Mặc định là 'PENDING' để xem các đơn đăng ký chờ duyệt.
     */
    @GetMapping("/admin/shops")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getShopsByStatus(@RequestParam(defaultValue = "PENDING") String status) {
        List<ShopResponse> shops = shopService.getShopsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gian hàng thành công", shops));
    }

    /**
     * Admin API: Phê duyệt gian hàng
     * Hệ thống sẽ tự động chuyển status sang ACTIVE và cấp vai trò ROLE_SELLER cho tài khoản người sở hữu.
     */
    @PatchMapping("/admin/shops/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShopResponse>> approveShop(@PathVariable Long id) {
        ShopResponse shop = shopService.approveShop(id);
        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt gian hàng thành công", shop));
    }

    /**
     * Admin API: Từ chối đơn đăng ký mở gian hàng kèm lý do
     */
    @PatchMapping("/admin/shops/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShopResponse>> rejectShop(@PathVariable Long id, @RequestParam String reason) {
        ShopResponse shop = shopService.rejectShop(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu mở gian hàng", shop));
    }

    /**
     * Admin API: Tạm khóa gian hàng do vi phạm quy định
     */
    @PatchMapping("/admin/shops/{id}/suspend")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ShopResponse>> suspendShop(@PathVariable Long id, @RequestParam String reason) {
        ShopResponse shop = shopService.suspendShop(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã tạm khóa gian hàng", shop));
    }
}