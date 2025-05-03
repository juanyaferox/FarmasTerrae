package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    ProductsService productsService;

    @Autowired
    OrderService orderService;

    @Autowired
    ReviewMapper reviewMapper;

    @Autowired
    ProductMapper productMapper;

    private static final int PAGE_SIZE_USER = 10;

    @Transactional (readOnly = true)
    public Page<ReviewDTO> getReviews(User user, int page) {
        Pageable pageable = Pageable
                .ofSize(PAGE_SIZE_USER)
                .withPage(page);

        return reviewRepository.findByUser(user, pageable)
                .map(reviewMapper::reviewToReviewDTO);
    }

    //@Transactional
    public ReviewDTO getReview(long idUser, long idReview) throws ResponseStatusException {

        Review review = getReviewDB(idReview);

        validateReview(idReview, idUser);

        return reviewMapper.reviewToReviewDTO(review);
    }

    private Review getReviewDB(long idReview) throws ResponseStatusException {
        return reviewRepository.findById(idReview)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    // Muy importante solo ser llamado despues del getReview para asegurar de que tiene acceso al recurso
    @Transactional
    public ProductDTO getProductFromReview(long idReview) throws ResponseStatusException {
        Review review = getReviewDB(idReview);
        return productsService.getProductDTOByReference(review.getProduct().getReference());
    }

    public void deleteReview(long idReview, User user) throws ResponseStatusException {

        validateReview(idReview, user.getId());

        reviewRepository.deleteById(idReview);
    }

    public Review setReview(ReviewDTO reviewDTO, User user) throws ResponseStatusException {

        Product product =
                productsService.getProductByReference(reviewDTO.getProductReference());

        Review review = reviewMapper.reviewDTOToReview(reviewDTO);
        review.setProduct(product);
        review.setUser(user);

        return reviewRepository.save(review);
    }

    public Review updateReview(ReviewDTO reviewDTO, User user) {

        Review review = validateReview(reviewDTO.getId(), user.getId());

        reviewMapper.updateReviewFromReviewDTO(reviewDTO, review);

        return reviewRepository.save(review);
    }

    private Review validateReview(long idReview, long idUser) throws ResponseStatusException {

        Review review = reviewRepository.findById(idReview).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.")
        );

        if (!review.getUser().getId().equals(idUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
        }
        return review;
    }

    @Transactional
    public List<ProductDTO> getProductForReview(User user) {

        return orderService.getProductsFromUserOrders(user);
        // verificar que los productos no tengan reseña
    }
}
