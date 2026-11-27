package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.CreateReviewDTO;
import com.iyg16260.farmasterrae.dto.user.UpdateReviewDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import com.iyg16260.farmasterrae.utils.PageableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

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

    /** Columnas por las que se puede ordenar el listado de reseñas del panel de usuario */
    static final Set<String> SORTABLE_FIELDS = Set.of("title", "content", "scoring");

    /**
     * Obtiene una página de reviews del usuario
     *
     * @param user usuario del que obtener las reviews
     * @param page número de página; fuera de rango se ajusta a la primera o a la última
     * @param sort columna de ordenación; si no está en {@link #SORTABLE_FIELDS} se usa el orden por defecto
     * @param dir  dirección de la ordenación ({@code asc} o {@code desc})
     * @return página de reviews DTO del usuario
     */
    @Transactional (readOnly = true)
    public Page<ReviewDTO> getReviews(User user, int page, String sort, String dir) {
        return PageableUtils.findClampedPage(page, PAGE_SIZE_USER, PageableUtils.sortOf(sort, dir, SORTABLE_FIELDS),
                        pageable -> reviewRepository.findByUser(user, pageable))
                .map(reviewMapper::reviewToReviewDTO);
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
    public Review setReview(CreateReviewDTO reviewDTO, User user) throws ResponseStatusException {

        Product product =
                productsService.getProductByReference(reviewDTO.getProductReference());

        if (!orderService.getProductsFromUserOrders(user).stream().anyMatch(p -> p.getId().equals(product.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes reseñar productos de pedidos completados");
        }
        if (reviewRepository.existsByUser_IdAndProduct_Id(user.getId(), product.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has reseñado este producto");
        }

        Review review = reviewMapper.createReviewDTOToReview(reviewDTO);
        review.setId(null);
        review.setProduct(product);
        review.setUser(user);

        try {
            return reviewRepository.save(review);
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has reseñado este producto", duplicate);
        }
    }

    /**
     * Actualiza la información de una review a partir del DTO
     *
     * @param reviewDTO DTO con los datos actualizados
     * @param user      usuario que actualiza la review
     * @return review actualizada
     * @throws ResponseStatusException FORBIDDEN si no tiene permisos
     */
    public Review updateReview(UpdateReviewDTO reviewDTO, User user) throws ResponseStatusException {

        Review review = obtainAndvalidateReview(reviewDTO.getId(), user.getId());

        reviewMapper.updateReviewFromUpdateDTO(reviewDTO, review);

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
    @Transactional (readOnly = true)
    public List<ProductDTO> getProductForReview(User user) {
        Set<Long> reviewedProductIds = reviewRepository.findReviewedProductIdsByUserId(user.getId());
        return orderService.getProductsFromUserOrders(user).stream()
                .filter(product -> product != null && product.getDeletedAt() == null)
                .filter(product -> !reviewedProductIds.contains(product.getId()))
                .map(productMapper::productToProductDTO)
                .toList();
    }
}
