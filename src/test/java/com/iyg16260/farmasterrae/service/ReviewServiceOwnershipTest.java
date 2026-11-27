package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.CreateReviewDTO;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceOwnershipTest {
    @Mock ReviewRepository reviewRepository;
    @Mock ProductsService productsService;
    @Mock OrderService orderService;
    @Mock ReviewMapper reviewMapper;
    @Mock ProductMapper productMapper;
    @InjectMocks ReviewService reviewService;

    private User user;
    private Product product;
    private CreateReviewDTO request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        product = new Product();
        product.setId(31L);
        product.setReference("REF31");
        request = new CreateReviewDTO();
        request.setTitle("Good");
        request.setContent("Works well");
        request.setScoring(8);
        request.setProductReference("REF31");
    }

    @Test
    void createAlwaysClearsEntityIdBeforeSaving() {
        Review mapped = new Review();
        mapped.setId(900L);
        when(productsService.getProductByReference("REF31")).thenReturn(product);
        when(orderService.getProductsFromUserOrders(user)).thenReturn(Set.of(product));
        when(reviewMapper.createReviewDTOToReview(request)).thenReturn(mapped);
        when(reviewRepository.save(mapped)).thenReturn(mapped);

        Review saved = reviewService.setReview(request, user);

        assertThat(saved.getId()).isNull();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getProduct()).isSameAs(product);
        verify(reviewRepository).save(mapped);
    }

    @Test
    void createRejectsProductWithoutCompletedPurchase() {
        when(productsService.getProductByReference("REF31")).thenReturn(product);
        when(orderService.getProductsFromUserOrders(user)).thenReturn(Set.of());

        assertThatThrownBy(() -> reviewService.setReview(request, user))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createRejectsExistingReviewWithConflict() {
        when(productsService.getProductByReference("REF31")).thenReturn(product);
        when(orderService.getProductsFromUserOrders(user)).thenReturn(Set.of(product));
        when(reviewRepository.existsByUser_IdAndProduct_Id(7L, 31L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.setReview(request, user))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(reviewRepository, never()).save(any());
    }
}
