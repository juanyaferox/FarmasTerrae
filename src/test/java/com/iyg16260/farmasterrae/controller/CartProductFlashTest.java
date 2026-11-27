package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.ProductsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class CartProductFlashTest {
    private CartService cartService;
    private ProductsService productsService;
    private MockMvc cartMvc;
    private MockMvc productsMvc;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        productsService = mock(ProductsService.class);
        CartController cartController = new CartController();
        ReflectionTestUtils.setField(cartController, "cartService", cartService);
        cartMvc = MockMvcBuilders.standaloneSetup(cartController).build();

        ProductsController productsController = new ProductsController();
        ReflectionTestUtils.setField(productsController, "productsService", productsService);
        productsMvc = MockMvcBuilders.standaloneSetup(productsController).build();
    }

    @Test
    void outOfStockAddShowsErrorFlashAndNeverSuccessFlash() throws Exception {
        when(cartService.addProductToCart(org.mockito.ArgumentMatchers.eq("sample"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
        var result = cartMvc.perform(post("/cart/add/sample"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/sample"))
                .andExpect(flash().attribute("errorMessage", "No hay stock disponible para añadir este producto."))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getFlashMap().keySet()).doesNotContain("added", "successMessage");
    }

    @Test
    void productDetailsIgnoresAddedAndErrorQueryParameters() throws Exception {
        ProductDTO product = new ProductDTO();
        product.setName("Producto");
        product.setReference("sample");
        product.setPrice(BigDecimal.ONE);
        product.setStock(0);
        when(productsService.getProductDTOByReference("sample")).thenReturn(product);
        when(productsService.getReviewsFromProduct("sample")).thenReturn(List.of());

        productsMvc.perform(get("/products/sample").param("added", "true").param("error", "phishing"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/product-details"))
                .andExpect(model().attributeDoesNotExist("added", "error"));
    }

    @Test
    void inStockAddSetsAddedFlashOnlyAndUrlEncodesReference() throws Exception {
        when(cartService.addProductToCart(org.mockito.ArgumentMatchers.eq("ref with space"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        var result = cartMvc.perform(post("/cart/add/ref with space"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/ref%20with%20space"))
                .andExpect(flash().attribute("added", true))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getFlashMap().keySet()).doesNotContain("successMessage", "errorMessage");
    }
}
