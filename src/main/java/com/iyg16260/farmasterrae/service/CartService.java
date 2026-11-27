package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CartService {
    public static final String REMOVED_PRODUCT_NOTICE = "removedProductNotice";

    @Autowired
    ProductsService productsService;

    @Autowired
    StockReservationService stockReservationService;

    public static final String CART_SESSION_KEY = "cart";

    /**
     * Obtiene el carrito a partir de la sesión de forma segura frente a concurrencia
     *
     * @param session sesión http
     * @return carrito con los productos y cantidad
     */
    public SessionCart getCart(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cartObject = (SessionCart) session.getAttribute(CART_SESSION_KEY);
            if (cartObject == null) {
                SessionCart cart = new SessionCart();
                session.setAttribute(CART_SESSION_KEY, cart);
                return cart;
            }
            return cartObject;
        }
    }

    /**
     * Añade un producto al carrito e intenta reservar el stock
     *
     * @param refProduct referencia del producto a añadir
     * @param session    sesión http
     * @return éxito de la operación
     * @throws ResponseStatusException si no se encuentra el producto
     */
    public boolean addProductToCart(String refProduct, HttpSession session) throws ResponseStatusException {
        productsService.getProductByReference(refProduct);

        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = getCart(session);
            cart.addProduct(refProduct);

            var result = stockReservationService.reserveStockForCartWithResult(session, cart);
            if (result == StockReservationService.ReservationResult.INSUFFICIENT_STOCK) {
                cart.deleteOneProduct(refProduct);
                return false;
            }
            return true;
        }
    }

    /**
     * Elimina una unidad de un producto del carrito (solo si existe en el carrito) y actualiza la reserva
     *
     * @param refProduct referencia del producto a borrar
     * @param session    sesión http
     * @return true si se actualizó la reserva correctamente, false si el producto no estaba en el carrito o falló la reserva
     */
    public boolean deleteProductFromCart(String refProduct, HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = getCart(session);
            if (!cart.containsProduct(refProduct)) {
                return false;
            }
            cart.deleteOneProduct(refProduct);
            return stockReservationService.reserveStockForCartWithResult(session, cart)
                    != StockReservationService.ReservationResult.INSUFFICIENT_STOCK;
        }
    }

    /**
     * Elimina todos los productos del mismo tipo del carrito y actualiza la reserva
     *
     * @param refProduct referencia del producto a borrar completamente del carrito
     * @param session    sesión http
     * @return true si se actualizó la reserva correctamente, false si el producto no estaba en el carrito o falló la reserva
     */
    public boolean deleteAllSameProductFromCart(String refProduct, HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = getCart(session);
            if (!cart.containsProduct(refProduct)) {
                return false;
            }
            cart.deleteAllSameProduct(refProduct);
            return stockReservationService.reserveStockForCartWithResult(session, cart)
                    != StockReservationService.ReservationResult.INSUFFICIENT_STOCK;
        }
    }

    /**
     * Limpia el carrito y libera todas las reservas y atributos de sesión asociados
     *
     * @param session sesión http
     */
    public void clearCart(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = getCart(session);
            cart.clear();

            String reservationId = stockReservationService.getReservationId(session);
            if (reservationId != null) {
                stockReservationService.releaseReservation(reservationId);
            }
            stockReservationService.clearSessionReservationAttributes(session);
        }
    }

    /**
     * Tamaño del carrito
     *
     * @param session sesión http
     * @return cantidad de productos distintos del carrito
     */
    public int getCartSize(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getSize();
    }

    /**
     * Verifica si las reservas de stock son válidas (vigentes, existentes en memoria y coincidentes con el carrito)
     *
     * @return true si todas las reservas son válidas o pudieron renovarse, false en caso contrario
     */
    public boolean validateCartReservations(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = getCart(session);
            if (cart.getProducts().isEmpty()) {
                return false;
            }
            return stockReservationService.reserveStockForCart(session, cart);
        }
    }

    /**
     * Carrito pero con el productDTO en lugar de la referencia
     *
     * @param session sesión http
     * @return map de producto y cantidad en orden determinista
     */
    public Map<ProductDTO, Integer> getDetailedProducts(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = getCart(session);
            Map<ProductDTO, Integer> detailed = new LinkedHashMap<>();
            boolean removed = false;
            for (Map.Entry<String, Integer> entry : cart.getProducts().entrySet()) {
                try {
                    detailed.put(productsService.getProductDTOByReference(entry.getKey()), entry.getValue());
                } catch (ResponseStatusException ex) {
                    if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                        throw ex;
                    }
                    cart.deleteAllSameProduct(entry.getKey());
                    session.setAttribute(REMOVED_PRODUCT_NOTICE, true);
                    removed = true;
                }
            }
            if (removed) {
                stockReservationService.reserveStockForCartWithResult(session, cart);
            }
            return detailed;
        }
    }
}
