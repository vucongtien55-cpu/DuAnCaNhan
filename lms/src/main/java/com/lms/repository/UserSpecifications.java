package com.lms.repository;

import com.lms.entity.Enrollment;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Điều kiện tìm kiếm người dùng. Mọi giá trị đi qua tham số của truy vấn, không nối chuỗi vào câu lệnh. */
public final class UserSpecifications {

    private static final int MAX_KEYWORD_LENGTH = 100;
    /** Ký tự thoát của LIKE (dấu gạch chéo ngược). */
    private static final char ESCAPE = (char) 92;

    private UserSpecifications() {
    }

    /** Khớp một phần, không phân biệt hoa thường, trên tên đăng nhập, họ tên hoặc email. */
    public static Specification<User> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String trimmed = keyword.trim();
        String pattern = "%" + escapeLike(trimmed.substring(0, Math.min(trimmed.length(), MAX_KEYWORD_LENGTH))
                .toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("fullName")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("email")), pattern, ESCAPE));
    }

    public static Specification<User> hasRole(RoleName role) {
        if (role == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var r = sub.correlate(root).join("roles");
            sub.select(cb.literal(1L)).where(cb.equal(r.get("name"), role));
            return cb.exists(sub);
        };
    }

    public static Specification<User> hasEnabled(Boolean enabled) {
        if (enabled == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    /** Chưa có đăng ký còn hiệu lực (khác DROPPED) trong lớp; sinh viên đã rút vẫn được thêm lại. */
    public static Specification<User> notEnrolledIn(Long sectionId) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var e = sub.from(Enrollment.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(e.get("student"), root),
                    cb.equal(e.get("section").get("id"), sectionId),
                    cb.notEqual(e.get("status"), EnrollmentStatus.DROPPED));
            return cb.not(cb.exists(sub));
        };
    }

    /** Ký tự đại diện của LIKE trong từ khóa do người dùng nhập phải được hiểu là ký tự thường. */
    static String escapeLike(String text) {
        String escape = String.valueOf(ESCAPE);
        return text.replace(escape, escape + escape).replace("%", escape + "%").replace("_", escape + "_");
    }
}
