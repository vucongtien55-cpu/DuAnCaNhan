package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Review;

import java.util.List;

public interface ReviewService {

    // Lấy đánh giá sản phẩm, có thể lọc theo số sao.
    List<Review> getProductReviews(Long productId, Integer rating);

    // Lọc đánh giá sản phẩm từ mức sao tối thiểu trở lên.
    List<Review> getProductReviewsByMinimumRating(Long productId, Integer minimumRating);

    // Lấy đánh giá của chính người dùng.
    List<Review> getMyReviews(Long userId);

    // Chỉ khách đã nhận sản phẩm mới được viết đánh giá.
    Review createReview(Long userId, Long productId, Integer rating, String comment);

    // Chỉ chủ đánh giá được sửa nội dung và số sao.
    Review updateReview(Long userId, Long reviewId, Integer rating, String comment);

    // Chỉ chủ đánh giá được xóa đánh giá.
    void deleteReview(Long userId, Long reviewId);
}
