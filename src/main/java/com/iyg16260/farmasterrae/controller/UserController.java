package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ReviewService;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@RestController
@RequestMapping ("/api/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    OrderService orderService;

    @Autowired
    ReviewService reviewService;

    /**
     * Pedidos del usuario
     *
     * @param page página actual, no obligatoria
     * @param sort campo por el que ordenar, no obligatorio
     * @param dir  dirección de la ordenación (asc/desc)
     * @return página de pedidos
     */
    @GetMapping ("/orders")
    public PagedModel<OrderDTO> getOrders(@AuthenticationPrincipal User user,
                                          @RequestParam (defaultValue = "0", required = false) int page,
                                          @RequestParam (required = false) String sort,
                                          @RequestParam (defaultValue = "asc") String dir) {
        Page<OrderDTO> orders = orderService.getOrders(user, page, sort, dir);
        return new PagedModel<>(orders);
    }

    /**
     * Detalles de un pedido con la informacion del pedido y una lista de sus productos
     *
     * @param idOrder id del pedido
     * @param user    usuario autenticado
     * @return detalles del pedido
     */
    @GetMapping ("/orders/{idOrder}")
    public OrderDetailsDTO getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user) {
        return orderService.getOrder(user.getId(), idOrder);
    }

    /**
     * Información del usuario
     *
     * @param user usuario autenticado
     * @return datos del usuario
     */
    @GetMapping ("/info")
    public UserDTO getInfoUser(@AuthenticationPrincipal User user) {
        return userService.getUserById(user.getId());
    }

    /**
     * Cambia la informacion del usuario recibiendo el userDto
     *
     * @param user    usuario autenticado
     * @param userdto nuevos datos del usuario
     * @return mensaje de éxito
     */
    @PutMapping ("/info")
    public MessageResponseDTO setInfoUser(@AuthenticationPrincipal User user, @RequestBody UserDTO userdto) {
        userService.updateUserById(user.getId(), userdto);
        return buildSuccessMessage(EntityType.USERS, Operation.PUT);
    }

    /**
     * Reseñas del usuario
     *
     * @param page página actual, no obligatoria
     * @param sort campo por el que ordenar, no obligatorio
     * @param dir  dirección de la ordenación (asc/desc)
     * @return página de reseñas
     */
    @GetMapping ("/reviews")
    public PagedModel<ReviewDTO> getReviews(@AuthenticationPrincipal User user,
                                            @RequestParam (defaultValue = "0", required = false) int page,
                                            @RequestParam (required = false) String sort,
                                            @RequestParam (defaultValue = "asc") String dir) {
        Page<ReviewDTO> reviews = reviewService.getReviews(user, page, sort, dir);
        return new PagedModel<>(reviews);
    }

    /**
     * Productos comprados por el usuario que todavía no ha reseñado
     *
     * @param user usuario autenticado
     * @return listado de productos
     */
    @GetMapping ("/reviews/products")
    public List<ProductDTO> getProductsForReview(@AuthenticationPrincipal User user) {
        return reviewService.getProductForReview(user);
    }

    @PostMapping ("/reviews")
    @ResponseStatus (HttpStatus.CREATED)
    public MessageResponseDTO addReview(@AuthenticationPrincipal User user,
                                        @Valid @RequestBody ReviewDTO reviewDTO) {
        reviewService.setReview(reviewDTO, user);
        return buildSuccessMessage(EntityType.REVIEWS, Operation.POST);
    }

    @PutMapping ("/reviews/{idReview}")
    public MessageResponseDTO updateReview(@AuthenticationPrincipal User user,
                                           @PathVariable long idReview,
                                           @Valid @RequestBody ReviewDTO reviewDTO) {
        reviewDTO.setId(idReview);
        reviewService.updateReview(reviewDTO, user);
        return buildSuccessMessage(EntityType.REVIEWS, Operation.PUT);
    }

    @DeleteMapping ("/reviews/{idReview}")
    public MessageResponseDTO deleteReview(@PathVariable long idReview,
                                           @AuthenticationPrincipal User user) {
        reviewService.deleteReview(idReview, user);
        return buildSuccessMessage(EntityType.REVIEWS, Operation.DELETE);
    }
}
