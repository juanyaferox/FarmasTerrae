package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    ProductsService productsService;

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
        cart.addProduct(product.getReference());
    }

    public void deleteProductFromCart (String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteOneProduct(refProduct);
    }

    public void deleteAllSameProductFromCart (String refProduct, HttpSession session) {
        SessionCart cart = getCart(session);
        cart.deleteAllSameProduct(refProduct);
    }

    public void clearCart (HttpSession session) {
        SessionCart cart = getCart(session);
        cart.clear();
    }

    public int getCartSize (HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getSize();
    }

    public List<ProductDTO> getDetailedProducts(HttpSession session) {
        SessionCart cart = getCart(session);
        return cart.getProducts().keySet().stream()
                .map(reference -> productsService.getProductDTOByReference(reference))
                .toList();
    }
}
