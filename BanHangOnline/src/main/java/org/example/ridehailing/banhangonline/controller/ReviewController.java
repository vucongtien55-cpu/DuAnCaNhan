package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api")
@Transactional
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    // Đọc đánh giá của sản phẩm, có thể lọc theo số sao.
    @GetMapping("/products/{productId}/reviews")
    public List<ApiResponseDtos.ReviewResponse> getProductReviews(
            @PathVariable Long productId, @RequestParam(required = false) Integer rating) {
        return ApiDtoMapper.map(reviewService.getProductReviews(productId, rating), ApiDtoMapper::toResponse);
    }

    // Lọc review có số sao lớn hơn hoặc bằng minimumRating.
    @GetMapping("/products/{productId}/reviews/minimum-rating")
    public List<ApiResponseDtos.ReviewResponse> getProductReviewsByMinimumRating(
            @PathVariable Long productId, @RequestParam Integer minimumRating) {
        return ApiDtoMapper.map(
                reviewService.getProductReviewsByMinimumRating(productId, minimumRating),
                ApiDtoMapper::toResponse);
    }

    // Lấy đánh giá của principal hiện tại.
    @GetMapping("/users/me/reviews")
    public List<ApiResponseDtos.ReviewResponse> getMyReviews() {
        return ApiDtoMapper.map(
                reviewService.getMyReviews(currentUserService.requireCurrentUserId()), ApiDtoMapper::toResponse);
    }

    // Tạo đánh giá gắn với danh tính principal, không nhận userId trong payload.
    @PostMapping("/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.ReviewResponse createReview(
            @PathVariable Long productId, @Valid @RequestBody ApiRequestDtos.ReviewRequest request) {
        return ApiDtoMapper.toResponse(reviewService.createReview(
                currentUserService.requireCurrentUserId(), productId, request.rating(), request.comment()));
    }

    // Sửa đánh giá do principal hiện tại sở hữu.
    @PutMapping("/reviews/{reviewId}")
    public ApiResponseDtos.ReviewResponse updateReview(
            @PathVariable Long reviewId, @Valid @RequestBody ApiRequestDtos.ReviewRequest request) {
        return ApiDtoMapper.toResponse(reviewService.updateReview(
                currentUserService.requireCurrentUserId(), reviewId, request.rating(), request.comment()));
    }

    // Xóa đánh giá do principal hiện tại sở hữu.
    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(currentUserService.requireCurrentUserId(), reviewId);
    }
}
