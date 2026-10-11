package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Review;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.exception.DuplicateResourceException;
import org.example.ridehailing.banhangonline.repository.OrderItemRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.ReviewRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.ReviewService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final int COMMENT_MAX_LENGTH = 2000;
    private static final String DELIVERED_STATUS = "DELIVERED";

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Review> getProductReviews(Long productId, Integer rating) {
        requirePositiveId(productId, "productId");
        productRepository.findByIdAndActiveTrueAndShop_Status(productId, "ACTIVE")
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm đang hoạt động."));
        if (rating == null) {
            return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId);
        }
        validateRating(rating);
        return reviewRepository.findByProduct_IdAndRatingOrderByCreatedAtDesc(productId, rating);
    }

    @Override
    public List<Review> getProductReviewsByMinimumRating(Long productId, Integer minimumRating) {
        requirePositiveId(productId, "productId");
        validateRating(minimumRating);
        productRepository.findByIdAndActiveTrueAndShop_Status(productId, "ACTIVE")
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm đang hoạt động."));
        return reviewRepository.findByProduct_IdAndRatingGreaterThanEqualOrderByCreatedAtDesc(
                productId, minimumRating);
    }

    @Override
    public List<Review> getMyReviews(Long userId) {
        getEnabledUser(userId);
        return reviewRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Review createReview(Long userId, Long productId, Integer rating, String comment) {
        User user = getEnabledUser(userId);
        requirePositiveId(productId, "productId");
        validateRating(rating);
        validateComment(comment);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
        if (!orderItemRepository.existsByProduct_IdAndShopOrder_Order_Customer_IdAndShopOrder_Status(
                productId, userId, DELIVERED_STATUS)) {
            throw new AccessDeniedException("Chỉ khách đã nhận sản phẩm mới được đánh giá.");
        }
        if (reviewRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new DuplicateResourceException("Bạn đã đánh giá sản phẩm này.");
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(normalizeComment(comment));
        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public Review updateReview(Long userId, Long reviewId, Integer rating, String comment) {
        getEnabledUser(userId);
        requirePositiveId(reviewId, "reviewId");
        validateRating(rating);
        validateComment(comment);

        Review review = getReviewForManagement(userId, reviewId);
        review.setRating(rating);
        review.setComment(normalizeComment(comment));
        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        getEnabledUser(userId);
        requirePositiveId(reviewId, "reviewId");
        Review review = getReviewForManagement(userId, reviewId);
        reviewRepository.delete(review);
    }

    // ADMIN có thể kiểm duyệt; các vai trò khác chỉ sửa hoặc xóa đánh giá của chính mình.
    private Review getReviewForManagement(Long actorUserId, Long reviewId) {
        User actor = getEnabledUser(actorUserId);
        if ("ADMIN".equals(actor.getRole())) {
            return reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá."));
        }
        return reviewRepository.findByIdAndUser_Id(reviewId, actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá của người dùng."));
    }

    private User getEnabledUser(Long userId) {
        requirePositiveId(userId, "userId");
        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động."));
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao đánh giá phải nằm trong khoảng từ 1 đến 5.");
        }
    }

    private void validateComment(String comment) {
        if (comment != null && comment.length() > COMMENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Nội dung đánh giá không được vượt quá 2000 ký tự.");
        }
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
