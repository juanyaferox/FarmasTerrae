package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.ReviewService;
import com.iyg16260.farmasterrae.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:issue11reviewflowtestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class ReviewFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ReviewService reviewService;
    @Autowired private ProductsService productsService;
    @Autowired private UserService userService;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @MockitoBean private S3Presigner s3Presigner;

    @BeforeEach
    void stubPresigner() throws Exception {
        reset(s3Presigner);
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://signed.example/product-images/rev.jpg").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
    }

    private User createUser(String roleType) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User u = new User();
        u.setUsername("rev_" + suffix);
        u.setEmail("rev_" + suffix + "@example.com");
        u.setFullName("Reviewer " + suffix);
        u.setAddress("Calle Opinión 1");
        u.setPassword(passwordEncoder.encode("StrongPassword9!"));
        u.setProfile(profileRepository.findByType(roleType));
        return userRepository.saveAndFlush(u);
    }

    private Product createProduct(String imageKey) {
        String ref = "REV-" + UUID.randomUUID().toString().substring(0, 6);
        Product p = new Product();
        p.setName("Reviewable " + ref);
        p.setDescription("Desc");
        p.setPrice(1500);
        p.setStock(20);
        p.setReference(ref);
        p.setCategory(Category.CARE);
        p.setImagePath(imageKey);
        return productRepository.saveAndFlush(p);
    }

    private void createOrder(User buyer, Product product, SaleStatus status) {
        transactionTemplate.executeWithoutResult(tx -> {
            Order order = new Order();
            order.setUser(userRepository.getReferenceById(buyer.getId()));
            order.setStatus(status);
            order.setPaymentMethod(PaymentMethod.PAYPAL);
            order.setTotalPrice(new BigDecimal("15.00"));
            order.setName(buyer.getFullName());
            order.setAddress(buyer.getAddress());
            Order persisted = orderRepository.save(order);
            OrderDetails line = new OrderDetails(persisted, productRepository.getReferenceById(product.getId()));
            line.setAmount(1);
            line.setUnitPriceCents(product.getPrice());
            line.setProductName(product.getName());
            persisted.setOrderDetails(List.of(line));
            orderRepository.flush();
        });
    }

    @Test
    void postReviewCannotHijackForeignReviewIdAndPutDeleteEnforceOwnership() throws Exception {
        User victim = createUser("USER");
        User attacker = createUser("USER");
        Product product = createProduct("product-images/item.jpg");
        createOrder(victim, product, SaleStatus.COMPLETED);
        createOrder(attacker, product, SaleStatus.COMPLETED);

        Review victimReview = new Review();
        victimReview.setUser(victim);
        victimReview.setProduct(product);
        victimReview.setTitle("Original Victim Title");
        victimReview.setContent("Original Victim Content");
        victimReview.setScoring(9);
        victimReview = reviewRepository.saveAndFlush(victimReview);

        // Attacker tries to pass victim's review id on POST -> creates own review, victim's review untouched
        mockMvc.perform(post("/user/dashboard/reviews").with(user(attacker)).with(csrf())
                        .param("id", String.valueOf(victimReview.getId()))
                        .param("productReference", product.getReference())
                        .param("title", "Attacker Title")
                        .param("content", "Attacker Content")
                        .param("scoring", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard/reviews"));

        Review reloadedVictim = reviewRepository.findById(victimReview.getId()).orElseThrow();
        assertThat(reloadedVictim.getTitle()).isEqualTo("Original Victim Title");
        assertThat(reloadedVictim.getUser().getId()).isEqualTo(victim.getId());

        // Duplicate review by attacker on same product must return 409 Conflict
        mockMvc.perform(post("/user/dashboard/reviews").with(user(attacker)).with(csrf())
                        .param("productReference", product.getReference())
                        .param("title", "Second Attempt")
                        .param("content", "Duplicate")
                        .param("scoring", "5"))
                .andExpect(status().isConflict());

        // Attacker trying PUT or DELETE on victim's review gets 403 Forbidden
        mockMvc.perform(put("/user/dashboard/reviews").with(user(attacker)).with(csrf())
                        .param("id", String.valueOf(victimReview.getId()))
                        .param("title", "Hijacked")
                        .param("content", "Hijacked")
                        .param("scoring", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/user/dashboard/reviews").with(user(attacker)).with(csrf())
                        .param("idReview", String.valueOf(victimReview.getId())))
                .andExpect(status().isForbidden());

        assertThat(reviewRepository.findById(victimReview.getId())).isPresent();
    }

    @Test
    void enforcesCompletedPurchaseEligibilityAndLocalizedValidationMessages() throws Exception {
        User buyer = createUser("USER");
        Product pendingProduct = createProduct(null);
        Product cancelledProduct = createProduct(null);
        createOrder(buyer, pendingProduct, SaleStatus.PENDING);
        createOrder(buyer, cancelledProduct, SaleStatus.CANCELLED);

        mockMvc.perform(post("/user/dashboard/reviews").with(user(buyer)).with(csrf())
                        .param("productReference", pendingProduct.getReference())
                        .param("title", "Too Early")
                        .param("content", "Not delivered yet")
                        .param("scoring", "8"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/user/dashboard/reviews").with(user(buyer)).with(csrf())
                        .param("productReference", cancelledProduct.getReference())
                        .param("title", "Cancelled")
                        .param("content", "Cancelled order")
                        .param("scoring", "3"))
                .andExpect(status().isForbidden());

        // PUT with missing id and out-of-range scoring returns 400 with Spanish validation messages
        mockMvc.perform(put("/user/dashboard/reviews").with(user(buyer)).with(csrf())
                        .header("Referer", "/user/dashboard/reviews")
                        .param("title", "Ok")
                        .param("content", "Ok")
                        .param("scoring", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard/reviews"))
                .andExpect(flash().attributeExists("errorMessages"))
                .andExpect(result -> {
                    @SuppressWarnings("unchecked")
                    List<String> errors = (List<String>) result.getFlashMap().get("errorMessages");
                    assertThat(errors).isNotEmpty();
                    assertThat(errors).anyMatch(msg -> msg.contains("identificador"));
                    assertThat(errors).anyMatch(msg -> msg.contains("al menos 1"));
                    assertThat(errors).noneMatch(msg -> msg.contains("must "));
                });
    }

    @Test
    void getProductForReviewHandlesSoftDeletedReviewerExcludesArchivedProductsAndSignsThumbnails() {
        User admin = createUser("ADMIN");
        User deletedReviewer = createUser("USER");
        User activeBuyer = createUser("USER");
        Product activeProd = createProduct("product-images/thumb.jpg");
        Product archivedProd = createProduct("product-images/archived.jpg");

        createOrder(deletedReviewer, activeProd, SaleStatus.COMPLETED);
        createOrder(activeBuyer, activeProd, SaleStatus.COMPLETED);
        createOrder(activeBuyer, archivedProd, SaleStatus.COMPLETED);

        // Soft-deleted reviewer previously reviewed activeProd
        Review oldReview = new Review();
        oldReview.setUser(deletedReviewer);
        oldReview.setProduct(activeProd);
        oldReview.setTitle("Old");
        oldReview.setContent("From deleted user");
        oldReview.setScoring(8);
        reviewRepository.saveAndFlush(oldReview);

        // Soft-delete the first reviewer & archive archivedProd
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        admin, null, admin.getAuthorities()));
        try {
            userService.deleteUserById(deletedReviewer.getId());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        productsService.deleteProduct(archivedProd.getReference());

        // Must NOT throw EntityNotFoundException on soft-deleted reviewer, and must exclude archivedProd
        List<ProductDTO> eligible = reviewService.getProductForReview(activeBuyer);
        assertThat(eligible).extracting(ProductDTO::getReference)
                .containsExactly(activeProd.getReference());

        // Signing thumbnails works on ReviewDTO and falls back to placeholder on S3 error
        Review buyerReview = new Review();
        buyerReview.setUser(activeBuyer);
        buyerReview.setProduct(activeProd);
        buyerReview.setTitle("Mine");
        buyerReview.setContent("Great product");
        buyerReview.setScoring(10);
        reviewRepository.saveAndFlush(buyerReview);

        Page<ReviewDTO> page = reviewService.getReviews(activeBuyer, 0, null, "asc");
        assertThat(page.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getProductImage()).isEqualTo("https://signed.example/product-images/rev.jpg");
            assertThat(dto.getProductName()).isEqualTo(activeProd.getName());
        });

        // Fresh product with uncached key falls back to placeholder when presigner fails
        Product failingProd = createProduct("product-images/fail-sign.jpg");
        createOrder(activeBuyer, failingProd, SaleStatus.COMPLETED);
        Review secondReview = new Review();
        secondReview.setUser(activeBuyer);
        secondReview.setProduct(failingProd);
        secondReview.setTitle("Second");
        secondReview.setContent("Fallback test");
        secondReview.setScoring(7);
        reviewRepository.saveAndFlush(secondReview);

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("down").statusCode(503).build());

        Page<ReviewDTO> fallbackPage = reviewService.getReviews(activeBuyer, 0, null, "asc");
        assertThat(fallbackPage.getContent())
                .filteredOn(dto -> dto.getProductReference().equals(failingProd.getReference()))
                .singleElement()
                .extracting(ReviewDTO::getProductImage)
                .isEqualTo("/assets/img/placeholder.svg");
    }
}
