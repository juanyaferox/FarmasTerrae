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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;
import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@Tag (name = TAG_USER)
@SecurityRequirement (name = SESSION_COOKIE)
@ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
@ApiResponse (responseCode = "403", ref = FORBIDDEN)
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
    @io.swagger.v3.oas.annotations.Operation (summary = "Pedidos del usuario",
            description = "Listado paginado de los pedidos del usuario autenticado")
    @ApiResponse (responseCode = "200", description = "Página de pedidos")
    @GetMapping ("/orders")
    public PagedModel<OrderDTO> getOrders(@AuthenticationPrincipal User user,
                                          @Parameter (description = "Número de página, empieza en 0")
                                          @RequestParam (defaultValue = "0", required = false) int page,
                                          @Parameter (description = "Campo por el que ordenar", example = "createdAt")
                                          @RequestParam (required = false) String sort,
                                          @Parameter (description = "Dirección de la ordenación: asc o desc")
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
    @io.swagger.v3.oas.annotations.Operation (summary = "Detalle de un pedido",
            description = "Información del pedido y sus productos. Solo si el pedido es del usuario (o es ADMIN)")
    @ApiResponse (responseCode = "200", description = "Detalle del pedido")
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @GetMapping ("/orders/{idOrder}")
    public OrderDetailsDTO getOrder(@Parameter (description = "Id del pedido") @PathVariable int idOrder,
                                    @AuthenticationPrincipal User user) {
        return orderService.getOrder(user.getId(), idOrder);
    }

    /**
     * Información del usuario
     *
     * @param user usuario autenticado
     * @return datos del usuario
     */
    @io.swagger.v3.oas.annotations.Operation (summary = "Datos del usuario",
            description = "Datos personales del usuario autenticado")
    @ApiResponse (responseCode = "200", description = "Datos del usuario")
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
    @io.swagger.v3.oas.annotations.Operation (summary = "Actualizar los datos del usuario",
            description = "Actualiza los datos personales del usuario autenticado")
    @ApiResponse (responseCode = "200", description = "Usuario actualizado")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
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
    @io.swagger.v3.oas.annotations.Operation (summary = "Reseñas del usuario",
            description = "Listado paginado de las reseñas del usuario autenticado")
    @ApiResponse (responseCode = "200", description = "Página de reseñas")
    @GetMapping ("/reviews")
    public PagedModel<ReviewDTO> getReviews(@AuthenticationPrincipal User user,
                                            @Parameter (description = "Número de página, empieza en 0")
                                            @RequestParam (defaultValue = "0", required = false) int page,
                                            @Parameter (description = "Campo por el que ordenar", example = "scoring")
                                            @RequestParam (required = false) String sort,
                                            @Parameter (description = "Dirección de la ordenación: asc o desc")
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
    @io.swagger.v3.oas.annotations.Operation (summary = "Productos pendientes de reseñar",
            description = "Productos comprados por el usuario que todavía no ha reseñado")
    @ApiResponse (responseCode = "200", description = "Listado de productos")
    @GetMapping ("/reviews/products")
    public List<ProductDTO> getProductsForReview(@AuthenticationPrincipal User user) {
        return reviewService.getProductForReview(user);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Crear una reseña",
            description = "Crea una reseña del usuario sobre el producto indicado en productReference")
    @ApiResponse (responseCode = "201", description = "Reseña creada")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @PostMapping ("/reviews")
    @ResponseStatus (HttpStatus.CREATED)
    public MessageResponseDTO addReview(@AuthenticationPrincipal User user,
                                        @Valid @RequestBody ReviewDTO reviewDTO) {
        reviewService.setReview(reviewDTO, user);
        return buildSuccessMessage(EntityType.REVIEWS, Operation.POST);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Actualizar una reseña",
            description = "Actualiza una reseña del usuario autenticado. Responde 403 si no es suya o no existe")
    @ApiResponse (responseCode = "200", description = "Reseña actualizada")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @PutMapping ("/reviews/{idReview}")
    public MessageResponseDTO updateReview(@AuthenticationPrincipal User user,
                                           @Parameter (description = "Id de la reseña") @PathVariable long idReview,
                                           @Valid @RequestBody ReviewDTO reviewDTO) {
        reviewDTO.setId(idReview);
        reviewService.updateReview(reviewDTO, user);
        return buildSuccessMessage(EntityType.REVIEWS, Operation.PUT);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Eliminar una reseña",
            description = "Elimina una reseña del usuario autenticado. Responde 403 si no es suya o no existe")
    @ApiResponse (responseCode = "200", description = "Reseña eliminada")
    @DeleteMapping ("/reviews/{idReview}")
    public MessageResponseDTO deleteReview(@Parameter (description = "Id de la reseña") @PathVariable long idReview,
                                           @AuthenticationPrincipal User user) {
        reviewService.deleteReview(idReview, user);
        return buildSuccessMessage(EntityType.REVIEWS, Operation.DELETE);
    }
}
