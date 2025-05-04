package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CartService {

    @Autowired
    ProductsService productsService;

    public static final String CART_SESSION_KEY = "cart";

    public SessionCart getCart(HttpSession session) {
        SessionCart cartObject = (SessionCart) session.getAttribute(CART_SESSION_KEY);
        if (cartObject == null) {
            SessionCart cart = new SessionCart();
            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }
        return cartObject;
    }

    public void addProductToCart(String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.addProduct(refProduct);
    }

    public void deleteProductFromCart(String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteOneProduct(refProduct);
    }

    public void deleteAllSameProductFromCart(String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteAllSameProduct(refProduct);
    }

    public void clearCart(HttpSession session) {
        SessionCart cart = getCart(session);
        cart.clear();
    }

    public int getCartSize(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getSize();
    }

    /**
     * Map productDTO + cantidad en lugar de la referencia + cantidad
     *
     * @param session
     * @return
     */
    public Map<ProductDTO, Integer> getDetailedProducts(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getProducts()
                .entrySet().stream()
                .collect(Collectors.toMap(reference ->
                                productsService.getProductDTOByReference(reference.getKey()),
                        Map.Entry::getValue// q -> q.getValue
                ));
    }

    /**
     * Listado de productos en el carrito, segun la cantidad de cada
     *
     * @param session
     * @return
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
    
}
