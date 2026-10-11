package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Dùng để kiểm tra quyền quản trị khi sửa hoặc gỡ một đánh giá.
    Optional<Review> findByIdAndUser_Id(Long reviewId, Long userId);

    // Lấy đánh giá của người dùng, đánh giá mới nhất đứng trước.
    List<Review> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // Lấy đánh giá cho sản phẩm, đánh giá mới nhất đứng trước.
    List<Review> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    // Lọc đánh giá sản phẩm theo đúng số sao.
    List<Review> findByProduct_IdAndRatingOrderByCreatedAtDesc(Long productId, Integer rating);

    // Lấy đánh giá sản phẩm đạt ít nhất số sao chỉ định.
    List<Review> findByProduct_IdAndRatingGreaterThanEqualOrderByCreatedAtDesc(
            Long productId, Integer minimumRating);

    // Kiểm tra người dùng đã đánh giá sản phẩm hay chưa.
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

}
