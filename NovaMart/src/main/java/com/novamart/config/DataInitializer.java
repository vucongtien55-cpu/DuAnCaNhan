package com.novamart.config;

import com.novamart.entity.Category;
import com.novamart.entity.Role;
import com.novamart.entity.RoleName;
import com.novamart.entity.User;
import com.novamart.repository.CategoryRepository;
import com.novamart.repository.RoleRepository;
import com.novamart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Tự động khởi tạo dữ liệu ban đầu khi ứng dụng khởi chạy lần đầu:
 * 1. Khởi tạo 3 Role hệ thống: ROLE_CUSTOMER, ROLE_SELLER, ROLE_ADMIN
 * 2. Khởi tạo tài khoản Administrator mặc định để phê duyệt Shop & quản trị hệ thống
 * 3. Khởi tạo một số danh mục sản phẩm mẫu
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initDefaultAdmin();
        initDefaultCategories();
    }

    private void initRoles() {
        if (roleRepository.findByName(RoleName.ROLE_CUSTOMER).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(RoleName.ROLE_CUSTOMER)
                    .displayName("Khách hàng")
                    .description("Tài khoản người mua hàng")
                    .build());
        }
        if (roleRepository.findByName(RoleName.ROLE_SELLER).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(RoleName.ROLE_SELLER)
                    .displayName("Người bán hàng")
                    .description("Tài khoản có quyền mở Shop và đăng bán sản phẩm")
                    .build());
        }
        if (roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(RoleName.ROLE_ADMIN)
                    .displayName("Quản trị viên")
                    .description("Tài khoản quản trị toàn hệ thống")
                    .build());
        }
    }

    private void initDefaultAdmin() {
        String adminEmail = "admin@novamart.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElse(null);
            Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElse(null);

            Set<Role> roles = new HashSet<>();
            if (adminRole != null) roles.add(adminRole);
            if (customerRole != null) roles.add(customerRole);

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123456"))
                    .fullName("NovaMart Administrator")
                    .phone("0988888888")
                    .status("ACTIVE")
                    .emailVerified(true)
                    .roles(roles)
                    .build();

            userRepository.save(admin);
        }
    }

    private void initDefaultCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder()
                    .name("Thời trang & Phụ kiện")
                    .slug("thoi-trang-phu-kien")
                    .description("Quần áo, giày dép, phụ kiện thời trang nam nữ")
                    .active(true)
                    .build());

            categoryRepository.save(Category.builder()
                    .name("Điện tử & Công nghệ")
                    .slug("dien-tu-cong-nghe")
                    .description("Điện thoại, máy tính bảng, laptop và phụ kiện công nghệ")
                    .active(true)
                    .build());

            categoryRepository.save(Category.builder()
                    .name("Nhà cửa & Đời sống")
                    .slug("nha-cua-doi-song")
                    .description("Đồ gia dụng, nội thất, thiết bị nhà bếp")
                    .active(true)
                    .build());
        }
    }
}

