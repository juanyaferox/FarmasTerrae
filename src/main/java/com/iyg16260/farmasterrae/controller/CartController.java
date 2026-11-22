package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;
import com.iyg16260.farmasterrae.dto.response.CartResponseDTO;
import com.iyg16260.farmasterrae.dto.response.ErrorResponseDTO;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping ("/api/cart")
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * Acceder al carrito
     */
    @GetMapping
    public CartResponseDTO viewCart(HttpSession session) {
        return buildCartResponse(session);
    }

    /**
     * Añadir una unidad de un producto al carrito
     *
     * @return carrito actualizado, o 409 si no hay stock suficiente para reservar
     */
    @PostMapping ("/items/{reference}")
    public ResponseEntity<?> addToCart(@PathVariable String reference, HttpSession session) {
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
    @PostMapping ("/items/{reference}/decrease")
    public CartResponseDTO decreaseCart(@PathVariable String reference, HttpSession session) {
        cartService.deleteProductFromCart(reference, session);
        return buildCartResponse(session);
    }

    /**
     * Borrar todas las unidades de un producto del carrito
     */
    @DeleteMapping ("/items/{reference}")
    public CartResponseDTO deleteFromCart(@PathVariable String reference, HttpSession session) {
        cartService.deleteAllSameProductFromCart(reference, session);
        return buildCartResponse(session);
    }

    /**
     * Limpiar el carrito
     */
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
