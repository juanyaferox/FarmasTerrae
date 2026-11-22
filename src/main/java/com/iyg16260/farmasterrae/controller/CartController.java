package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;
import com.iyg16260.farmasterrae.dto.response.CartResponseDTO;
import com.iyg16260.farmasterrae.dto.response.ErrorResponseDTO;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.NOT_FOUND;
import static com.iyg16260.farmasterrae.config.OpenApiConfig.TAG_CART;

@Tag (name = TAG_CART)
@RestController
@RequestMapping ("/api/cart")
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * Acceder al carrito
     */
    @Operation (summary = "Ver el carrito",
            description = "Productos del carrito de la sesión con su cantidad y el precio total")
    @ApiResponse (responseCode = "200", description = "Contenido del carrito")
    @GetMapping
    public CartResponseDTO viewCart(HttpSession session) {
        return buildCartResponse(session);
    }

    /**
     * Añadir una unidad de un producto al carrito
     *
     * @return carrito actualizado, o 409 si no hay stock suficiente para reservar
     */
    @Operation (summary = "Añadir una unidad al carrito",
            description = "Añade una unidad del producto y reserva su stock durante un tiempo limitado")
    @ApiResponse (responseCode = "200", description = "Carrito actualizado",
            content = @Content (schema = @Schema (implementation = CartResponseDTO.class)))
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @ApiResponse (responseCode = "409", description = "No hay stock suficiente para reservar el producto",
            content = @Content (schema = @Schema (implementation = ErrorResponseDTO.class)))
    @PostMapping ("/items/{reference}")
    public ResponseEntity<?> addToCart(@Parameter (description = "Referencia del producto", example = "REFMED001")
                                       @PathVariable String reference, HttpSession session) {
        boolean added = cartService.addProductToCart(reference, session);

        if (!added)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponseDTO.of(HttpStatus.CONFLICT,
                            "No hay suficiente stock disponible para añadir el producto"));

        return ResponseEntity.ok(buildCartResponse(session));
    }

    /**
     * Borrar una unidad de un producto del carrito
     */
    @Operation (summary = "Quitar una unidad del carrito",
            description = "Quita una unidad del producto y actualiza la reserva de stock")
    @ApiResponse (responseCode = "200", description = "Carrito actualizado")
    @PostMapping ("/items/{reference}/decrease")
    public CartResponseDTO decreaseCart(@Parameter (description = "Referencia del producto", example = "REFMED001")
                                        @PathVariable String reference, HttpSession session) {
        cartService.deleteProductFromCart(reference, session);
        return buildCartResponse(session);
    }

    /**
     * Borrar todas las unidades de un producto del carrito
     */
    @Operation (summary = "Quitar un producto del carrito",
            description = "Quita todas las unidades del producto y actualiza la reserva de stock")
    @ApiResponse (responseCode = "200", description = "Carrito actualizado")
    @DeleteMapping ("/items/{reference}")
    public CartResponseDTO deleteFromCart(@Parameter (description = "Referencia del producto", example = "REFMED001")
                                          @PathVariable String reference, HttpSession session) {
        cartService.deleteAllSameProductFromCart(reference, session);
        return buildCartResponse(session);
    }

    /**
     * Limpiar el carrito
     */
    @Operation (summary = "Vaciar el carrito", description = "Vacía el carrito y libera la reserva de stock")
    @ApiResponse (responseCode = "200", description = "Carrito vacío")
    @DeleteMapping
    public CartResponseDTO clearCart(HttpSession session) {
        cartService.clearCart(session);
        return buildCartResponse(session);
    }

    /**
     * Construye la respuesta con el contenido del carrito y su precio total
     */
    private CartResponseDTO buildCartResponse(HttpSession session) {
        Map<ProductDTO, Integer> cart = cartService.getDetailedProducts(session);

        var totalAmount = GenericUtils.priceAmountCalc(cart);

        return new CartResponseDTO(ProductQuantityDTO.fromMap(cart), totalAmount);
    }

}
