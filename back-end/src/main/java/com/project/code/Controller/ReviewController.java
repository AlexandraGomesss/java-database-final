package com.project.code.Controller;

import com.project.code.Model.Customer;
import com.project.code.Model.Review;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            CustomerRepository customerRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public Map<String, List<ReviewResponse>> getAllReviews() {
        return Map.of("reviews", toResponses(reviewRepository.findAll()));
    }

    @GetMapping("/{storeId}/{productId}")
    public ResponseEntity<Map<String, Object>> getReviews(
            @PathVariable("storeId") long storeId,
            @PathVariable("productId") long productId
    ) {
        if (storeId <= 0 || productId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Store ID and product ID must be positive."));
        }

        List<Review> reviews = reviewRepository.findByStoreIdAndProductId(storeId, productId);
        return ResponseEntity.ok(Map.of("reviews", toResponses(reviews)));
    }

    private List<ReviewResponse> toResponses(List<Review> reviews) {
        return reviews.stream().map(this::toResponse).toList();
    }

    private ReviewResponse toResponse(Review review) {
        Long customerId = review.getCustomerId();
        String customerName = customerId == null
                ? "Unknown"
                : customerRepository.findById(customerId)
                .map(Customer::getName)
                .orElse("Unknown");

        return new ReviewResponse(review.getComment(), review.getRating(), customerName);
    }

    public record ReviewResponse(String comment, Integer rating, String customerName) {
    }
}