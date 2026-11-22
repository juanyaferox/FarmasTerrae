package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.payment.PaymentDetailsDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;
import com.iyg16260.farmasterrae.dto.response.CheckoutResponseDTO;
import com.iyg16260.farmasterrae.dto.response.EnumOptionDTO;
import com.iyg16260.farmasterrae.dto.response.ErrorResponseDTO;
import com.iyg16260.farmasterrae.dto.response.OrderCreatedResponseDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import com.iyg16260.farmasterrae.utils.SessionCart;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;

@Tag (name = TAG_ORDER)
@SecurityRequirement (name = SESSION_COOKIE)
@ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
@RestController
@RequestMapping ("/api/order")
public class OrderController {

    private static final String EMPTY_CART_MESSAGE = "El carrito está vacío";

    @Autowired
    OrderService orderService;

    @Autowired
    CartService cartService;

    /**
     * Genera una version inicial del pedido
     *
     * @param session sesión http
     * @param user    usuario autenticado
     * @return el listado de productos, los métodos de pago y los detalles del pago precargados
     */
    @io.swagger.v3.oas.annotations.Operation (summary = "Resumen del pedido",
            description = "Versión inicial del pedido a partir del carrito: productos, métodos de pago " +
                    "y detalles del pago precargados con los datos del usuario")
    @ApiResponse (responseCode = "200", description = "Resumen del pedido",
            content = @Content (schema = @Schema (implementation = CheckoutResponseDTO.class)))
    @ApiResponse (responseCode = "400", description = "El carrito está vacío",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    @ApiResponse (responseCode = "409", description = "No hay stock suficiente o la reserva ha expirado",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    @GetMapping
    public ResponseEntity<?> getOrder(HttpSession session, @AuthenticationPrincipal User user) {
        SessionCart cart = cartService.getCart(session);
        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of(HttpStatus.BAD_REQUEST, EMPTY_CART_MESSAGE));
        }
        // Verificar que las reservas de stock son válidas
        if (!cartService.validateCartReservations(session)) {
            // Si no son válidas, se devuelve un error para que revise el carrito
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponseDTO.of(HttpStatus.CONFLICT,
                            "Algunos productos no tienen suficiente stock " +
                                    "o tu reserva ha expirado. Por favor, revisa tu carrito."));
        }
        Map<ProductDTO, Integer> cartProducts = cartService.getDetailedProducts(session);
        BigDecimal totalAmount = GenericUtils.priceAmountCalc(cartProducts);

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO();
        paymentDetails.setAmount(totalAmount);
        paymentDetails.setAddress(user.getAddress());
        paymentDetails.setFullName(user.getFullName());

        List<EnumOptionDTO> paymentMethods = Arrays.stream(PaymentMethod.values())
                .map(method -> new EnumOptionDTO(method.name(), method.getValue()))
                .toList();

        return ResponseEntity.ok(
                new CheckoutResponseDTO(ProductQuantityDTO.fromMap(cartProducts), paymentMethods, paymentDetails));
    }

    /**
     * Confirma el pedido con el método de pago
     *
     * @param paymentDetails detalles del pago
     * @return 201 con el id del pedido creado y su ubicación
     */
    @io.swagger.v3.oas.annotations.Operation (summary = "Confirmar el pedido",
            description = "Crea el pedido con los productos del carrito, en estado pendiente, " +
                    "y confirma la reserva de stock")
    @ApiResponse (responseCode = "201", description = "Pedido creado",
            headers = @Header (name = "Location", description = "URL del detalle del pedido",
                    schema = @Schema (type = "string")),
            content = @Content (schema = @Schema (implementation = OrderCreatedResponseDTO.class)))
    @ApiResponse (responseCode = "400", description = "Errores de validación o carrito vacío",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    @ApiResponse (responseCode = "403", ref = FORBIDDEN)
    @ApiResponse (responseCode = "409", description = "No hay stock suficiente o la reserva ha expirado",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    @PostMapping ("/confirm")
    public ResponseEntity<?> getPayment(@Valid @RequestBody PaymentDetailsDTO paymentDetails,
                                        @AuthenticationPrincipal User user,
                                        HttpSession session) {

        // Verificar nuevamente las reservas de stock
        if (!cartService.validateCartReservations(session)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponseDTO.of(HttpStatus.CONFLICT,
                            "Algunos productos no tienen suficiente stock o tu reserva ha expirado. Por favor, revisa tu carrito."));
        }

        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of(HttpStatus.BAD_REQUEST, EMPTY_CART_MESSAGE));
        }

        // Intentar crear el pedido (esto verificará y confirmará las reservas de stock)
        var order = orderService.setOrder(user, cart, SaleStatus.PENDING, paymentDetails, session);

        String message = SuccessMessageUtils.buildSuccessMessage(EntityType.ORDERS, Operation.POST).message();

        return ResponseEntity.created(URI.create("/api/user/orders/" + order.getId()))
                .body(new OrderCreatedResponseDTO(order.getId(), message));
    }
}
