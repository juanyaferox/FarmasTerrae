package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    public static final String CART_SESSION_KEY = "cart";

    public SessionCart getCart (HttpSession session) {
        SessionCart cartObject = (SessionCart) session.getAttribute(CART_SESSION_KEY);
        if (cartObject == null) {
            SessionCart cart = new SessionCart();
            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }
        return cartObject;
    }

    public void addProductToCart (Product product, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.addProduct(product);
    }

    public void deleteProductFromCart (long idProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteOneProduct(idProduct);
    }

    public void deleteAllSameProductFromCart (long idProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteAllSameProduct(idProduct);
    }

    public void clearCart (HttpSession session) {
        SessionCart cart = getCart(session);
        cart.clear();
    }

    public void getCartSize (HttpSession session) {
        SessionCart cart = getCart(session);
        cart.getSize();
    }
}
