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
    
    public ReviewDTO getReview(long idUser, long idReview) throws ResponseStatusException {

        Review review = getReviewDB(idReview);

        obtainAndvalidateReview(idReview, idUser);

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

        obtainAndvalidateReview(idReview, user.getId());

        reviewRepository.deleteById(idReview);
    }

    /**
     * Guarda una nueva review en la bbdd
     *
     * @param reviewDTO
     * @param user
     * @return
     * @throws ResponseStatusException
     */
    public Review setReview(ReviewDTO reviewDTO, User user) throws ResponseStatusException {

        Product product =
                productsService.getProductByReference(reviewDTO.getProductReference());

        Review review = reviewMapper.reviewDTOToReview(reviewDTO);
        review.setProduct(product);
        review.setUser(user);

        return reviewRepository.save(review);
    }

    /**
     * Actualiza la info de review a partir del reviewdto
     *
     * @param reviewDTO
     * @param user
     * @return
     */
    public Review updateReview(ReviewDTO reviewDTO, User user) throws ResponseStatusException {

        Review review = obtainAndvalidateReview(reviewDTO.getId(), user.getId());

        reviewMapper.updateReviewFromReviewDTO(reviewDTO, review);

        return reviewRepository.save(review);
    }

    /**
     * Obtiene el Review y lo valida en base al usuario
     *
     * @param idReview
     * @param idUser
     * @return
     * @throws ResponseStatusException
     */
    private Review obtainAndvalidateReview(long idReview, long idUser) throws ResponseStatusException {

        Review review = reviewRepository.findById(idReview).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.")
        );

        if (!review.getUser().getId().equals(idUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
        }
        return review;
    }

    /**
     * Obtener lista de productos los cuales el usuario ha comprado y no ha reseñado
     *
     * @param user
     * @return
     */
    @Transactional
    public List<ProductDTO> getProductForReview(User user) {

        return orderService.getProductsFromUserOrders(user).stream()
                .filter(product -> product.getReviewList().stream()
                        .noneMatch(review -> review.getUser().equals(user)
                        )
                ).map(productMapper::productToProductDTO)
                .toList();
    }
}
