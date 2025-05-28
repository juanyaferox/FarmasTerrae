package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CartService {

    @Autowired
    ProductsService productsService;

    @Autowired
    StockReservationService stockReservationService;

    public static final String CART_SESSION_KEY = "cart";

    /**
     * Obtiene el carrito a partir de la sesión
     *
     * @param session sesión http
     * @return carrito con los productos y cantidad
     */
    public SessionCart getCart(HttpSession session) {
        SessionCart cartObject = (SessionCart) session.getAttribute(CART_SESSION_KEY);
        if (cartObject == null) {
            SessionCart cart = new SessionCart();
            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }
        return cartObject;
    }

    /**
     * Añade un producto al carrito y e intenta reservar el stock
     *
     * @param refProduct referencia del producto a añadir
     * @param session    sesión http
     * @return éxito de la operación
     * @throws ResponseStatusException si no se encuentra el producto
     */
    public boolean addProductToCart(String refProduct, HttpSession session) throws ResponseStatusException {
        // Primero verificamos que hay stock disponible (considerando reservas previas)
        Product product = productsService.getProductByReference(refProduct);

        SessionCart cart = getCart(session);

        // Añadimos temporalmente el producto al carrito para validar
        cart.addProduct(refProduct);

        // Intentamos reservar el stock
        boolean reserved = stockReservationService.reserveStockForCart(session, cart);

        if (!reserved) {
            // Si no se puede reservar, eliminamos el producto del carrito
            cart.deleteOneProduct(refProduct);
            return false;
        }
        return true;
    }

    /**
     * Elimina un producto del carrito y actualiza la reserva
     *
     * @param refProduct referencia del producto a borrar
     * @param session    sesión http
     */
    public void deleteProductFromCart(String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteOneProduct(refProduct);

        // Actualizamos la reserva con el nuevo contenido del carrito
        stockReservationService.reserveStockForCart(session, cart);
    }

    /**
     * Elimina todos los productos del mismo tipo del carrito y actualiza la reserva
     *
     * @param refProduct referencia del producto a borrar completamente del carrito
     * @param session    sesión http
     */
    public void deleteAllSameProductFromCart(String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteAllSameProduct(refProduct);

        // Actualizamos la reserva con el nuevo contenido del carrito
        stockReservationService.reserveStockForCart(session, cart);
    }

    /**
     * Limpia el carrito y libera todas las reservas
     *
     * @param session sesión http
     */
    public void clearCart(HttpSession session) {
        SessionCart cart = getCart(session);
        cart.clear();

        // Liberar la reserva de stock
        String reservationId = stockReservationService.getReservationId(session);
        if (reservationId != null) {
            stockReservationService.releaseReservation(reservationId);
        }
    }

    /**
     * Tamaño del carrito
     *
     * @param session sesión http
     * @return cantidad de productos del carrito
     */
    public int getCartSize(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getSize();
    }

    /**
     * Verifica si las reservas de stock son válidas
     *
     * @return true si todas las reservas son válidas, false si alguna ha expirado o no hay suficiente stock
     */
    public boolean validateCartReservations(HttpSession session) {
        // Verificar si la reserva ha expirado
        if (stockReservationService.isReservationExpired(session)) {
            // Intentar renovar la reserva
            SessionCart cart = getCart(session);
            return stockReservationService.reserveStockForCart(session, cart);
        }
        return true;
    }

    /**
     *
     */
    /**
     * Carrito pero con el productDTO en lugar de la referencia
     *
     * @param session sesión http
     * @return map de producto y cantidad
     */
    public Map<ProductDTO, Integer> getDetailedProducts(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getProducts()
                .entrySet().stream()
                .collect(Collectors.toMap(reference ->
                                productsService.getProductDTOByReference(reference.getKey()),
                        Map.Entry::getValue
                ));
    }

    /**
     * Listado de productos del carrito
     *
     * @param session sesión http
     * @return listado de productos
     */
    public List<Product> getProducts(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getProducts()
                .entrySet().stream()
                .flatMap(cartProducts ->
                        IntStream.range(0, cartProducts.getValue())
                                .mapToObj(quantity ->
                                        productsService.getProductByReference(cartProducts.getKey())))
                .toList();
    }

    /**

     */
    /**
     * Confirma la reserva y actualiza el stock real
     * Este método_ debe llamarse cuando se completa el pedido
     *
     * @param session sesión http
     * @return si la reserva se pudo hacer con éxito o no
     */
    public boolean confirmReservation(HttpSession session) {
        String reservationId = stockReservationService.getReservationId(session);
        if (reservationId == null) {
            return false;
        }

        // Validar que las reservas sean válidas antes de confirmar
        if (!validateCartReservations(session)) {
            return false;
        }

        return stockReservationService.confirmReservation(reservationId);
    }
}