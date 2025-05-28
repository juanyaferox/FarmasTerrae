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

    /**
     * Obtiene una página de reviews del usuario
     *
     * @param user usuario del que obtener las reviews
     * @param page número de página
     * @return página de reviews DTO del usuario
     */
    @Transactional (readOnly = true)
    public Page<ReviewDTO> getReviews(User user, int page) {
        Pageable pageable = Pageable
                .ofSize(PAGE_SIZE_USER)
                .withPage(page);

        return reviewRepository.findByUser(user, pageable)
                .map(reviewMapper::reviewToReviewDTO);
    }

    /**
     * Obtiene una review específica por su id y valida el acceso del usuario
     *
     * @param idUser   id del usuario que solicita la review
     * @param idReview id de la review a obtener
     * @return review DTO
     * @throws ResponseStatusException NOT_FOUND si no se encuentra la review, FORBIDDEN si no tiene permisos
     */
    public ReviewDTO getReview(long idUser, long idReview) throws ResponseStatusException {

        Review review = getReviewDB(idReview);

        obtainAndvalidateReview(idReview, idUser);

        return reviewMapper.reviewToReviewDTO(review);
    }

    /**
     * Obtiene una review por id desde la base de datos
     *
     * @param idReview id de la review
     * @return review encontrada
     * @throws ResponseStatusException NOT_FOUND si no se encuentra la review
     */
    private Review getReviewDB(long idReview) throws ResponseStatusException {
        return reviewRepository.findById(idReview)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    /**
     * Obtiene el producto asociado a una review
     * Importante: solo llamar después de validar el acceso con getReview
     *
     * @param idReview id de la review
     * @return producto DTO asociado a la review
     * @throws ResponseStatusException si no se encuentra la review
     */
    @Transactional
    public ProductDTO getProductFromReview(long idReview) throws ResponseStatusException {
        Review review = getReviewDB(idReview);
        return productsService.getProductDTOByReference(review.getProduct().getReference());
    }

    /**
     * Elimina una review por su id validando el acceso del usuario
     *
     * @param idReview id de la review a eliminar
     * @param user     usuario que solicita la eliminación
     * @throws ResponseStatusException FORBIDDEN si no tiene permisos
     */
    public void deleteReview(long idReview, User user) throws ResponseStatusException {

        obtainAndvalidateReview(idReview, user.getId());

        reviewRepository.deleteById(idReview);
    }

    /**
     * Guarda una nueva review en la base de datos
     *
     * @param reviewDTO DTO con los datos de la review
     * @param user      usuario que crea la review
     * @return review guardada
     * @throws ResponseStatusException si no se encuentra el producto
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
     * Actualiza la información de una review a partir del DTO
     *
     * @param reviewDTO DTO con los datos actualizados
     * @param user      usuario que actualiza la review
     * @return review actualizada
     * @throws ResponseStatusException FORBIDDEN si no tiene permisos
     */
    public Review updateReview(ReviewDTO reviewDTO, User user) throws ResponseStatusException {

        Review review = obtainAndvalidateReview(reviewDTO.getId(), user.getId());

        reviewMapper.updateReviewFromReviewDTO(reviewDTO, review);

        return reviewRepository.save(review);
    }

    /**
     * Obtiene una review y valida que el usuario tenga acceso a ella
     *
     * @param idReview id de la review
     * @param idUser   id del usuario
     * @return review validada
     * @throws ResponseStatusException FORBIDDEN si no tiene permisos o no se encuentra
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
     * Obtiene lista de productos que el usuario ha comprado pero no ha reseñado
     *
     * @param user usuario del que obtener los productos
     * @return lista de productos DTO disponibles para reseñar
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
