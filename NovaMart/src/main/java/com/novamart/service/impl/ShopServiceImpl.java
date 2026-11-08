package com.novamart.service.impl;

import com.novamart.dto.request.ApplyShopRequest;
import com.novamart.dto.response.ShopResponse;
import com.novamart.entity.Role;
import com.novamart.entity.RoleName;
import com.novamart.entity.Shop;
import com.novamart.entity.User;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.RoleRepository;
import com.novamart.repository.ShopRepository;
import com.novamart.repository.UserRepository;
import com.novamart.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public ShopResponse applyShop(String userEmail, ApplyShopRequest request) {
        User user = getUserByEmail(userEmail);

        // Quy tắc nghiệp vụ: Mỗi người dùng chỉ được đăng ký tối đa 1 Shop trong phiên bản MVP
        if (shopRepository.existsByOwnerId(user.getId())) {
            throw new BadRequestException("Tài khoản của bạn đã gửi yêu cầu hoặc đang sở hữu gian hàng!");
        }

        if (shopRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên gian hàng này đã được đăng ký!");
        }

        String slug = generateSlug(request.getName());
        if (shopRepository.existsBySlug(slug)) {
            throw new BadRequestException("Đường dẫn (slug) của tên gian hàng này đã bị trùng lặp, vui lòng chọn tên khác!");
        }

        Shop shop = Shop.builder()
                .owner(user)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .contactPhone(request.getContactPhone())
                .status("PENDING") // Trạng thái chờ Admin duyệt
                .build();

        return mapToResponse(shopRepository.save(shop));
    }

    @Override
    public ShopResponse getMyShop(String userEmail) {
        User user = getUserByEmail(userEmail);
        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa sở hữu gian hàng nào"));
        return mapToResponse(shop);
    }

    @Override
    public ShopResponse getShopBySlug(String slug) {
        Shop shop = shopRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gian hàng"));
        return mapToResponse(shop);
    }

    @Override
    public List<ShopResponse> getShopsByStatus(String status) {
        return shopRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopResponse approveShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gian hàng"));

        if (!"PENDING".equals(shop.getStatus())) {
            throw new BadRequestException("Gian hàng không ở trạng thái chờ duyệt!");
        }

        shop.setStatus("ACTIVE");
        shop.setApprovedAt(LocalDateTime.now());

        // Phê duyệt thành công: Tự động bổ sung vai trò ROLE_SELLER cho chủ tài khoản
        User owner = shop.getOwner();
        Role sellerRole = roleRepository.findByName(RoleName.ROLE_SELLER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_SELLER)
                        .displayName("Người bán hàng")
                        .description("Tài khoản có quyền đăng bán sản phẩm")
                        .build()));
        owner.getRoles().add(sellerRole);
        userRepository.save(owner);

        return mapToResponse(shopRepository.save(shop));
    }

    @Override
    @Transactional
    public ShopResponse rejectShop(Long shopId, String reason) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gian hàng"));

        shop.setStatus("REJECTED");
        shop.setAdminNote(reason);
        return mapToResponse(shopRepository.save(shop));
    }

    @Override
    @Transactional
    public ShopResponse suspendShop(Long shopId, String reason) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gian hàng"));

        shop.setStatus("SUSPENDED");
        shop.setAdminNote(reason);
        return mapToResponse(shopRepository.save(shop));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private ShopResponse mapToResponse(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .ownerId(shop.getOwner().getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .contactPhone(shop.getContactPhone())
                .status(shop.getStatus())
                .adminNote(shop.getAdminNote())
                .approvedAt(shop.getApprovedAt())
                .build();
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        String normalized = input.replace("đ", "d").replace("Đ", "D");
        String nowhitespace = Pattern.compile("[\\s+]").matcher(normalized).replaceAll("-");
        String decomposed = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(decomposed).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
    }
}