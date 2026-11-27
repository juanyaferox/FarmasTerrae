package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.StockReservationService;
import com.iyg16260.farmasterrae.utils.SessionCart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:issue10cartcheckouttestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class CartAndCheckoutFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StockReservationService stockReservationService;

    private User createBuyer() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User buyer = new User();
        buyer.setUsername("buyer_" + suffix);
        buyer.setEmail("buyer_" + suffix + "@example.com");
        buyer.setFullName("Buyer " + suffix);
        buyer.setAddress("Calle Compra " + suffix);
        buyer.setPassword(passwordEncoder.encode("StrongPassword9!"));
        buyer.setProfile(profileRepository.findByType("USER"));
        return userRepository.saveAndFlush(buyer);
    }

    private Product createProduct(int priceCents, int stock) {
        Product p = new Product();
        p.setName("Cart Prod " + UUID.randomUUID().toString().substring(0, 6));
        p.setDescription("Desc");
        p.setPrice(priceCents);
        p.setStock(stock);
        p.setReference("CRT-" + UUID.randomUUID().toString().substring(0, 8));
        p.setCategory(Category.CARE);
        return productRepository.saveAndFlush(p);
    }

    @ParameterizedTest
    @ValueSource (strings = {
            "/cart/add/REFVIT001",
            "/cart/remove/REFVIT001",
            "/cart/clear",
            "/cart/increase/REFVIT001",
            "/cart/decrease/REFVIT001"
    })
    void cartMutationEndpointsRejectGetWith405AndMissingCsrfWith403(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post(path))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutRejectsGetAndMissingCsrfAndValidPostInvalidatesSession() throws Exception {
        User buyer = createBuyer();
        // Login to obtain a real authenticated session
        var loginRes = mockMvc.perform(post("/auth").with(csrf())
                        .param("username", buyer.getUsername())
                        .param("password", "StrongPassword9!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // GET /logout must NOT log out (returns 4xx and session stays authenticated)
        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/user/dashboard").session(session))
                .andExpect(status().isOk());

        // POST /logout without CSRF must be rejected with 403 and session stays authenticated
        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/user/dashboard").session(session))
                .andExpect(status().isOk());

        // POST /logout with CSRF succeeds, redirects to /auth?logout, and invalidates session
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void decreaseOnAbsentOrBogusReferenceDoesNotAddItemOrBreakCart() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/decrease/NON_EXISTENT_REF").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        SessionCart cart = (SessionCart) session.getAttribute(CartService.CART_SESSION_KEY);
        assertThat(cart.getProducts()).isEmpty();

        // /cart renders cleanly with 200 OK
        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void increaseWhenOutOfStockFlashesErrorMessage() throws Exception {
        Product product = createProduct(500, 1);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add/" + product.getReference()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("added", true));

        mockMvc.perform(post("/cart/increase/" + product.getReference()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Cleanup reservation
        mockMvc.perform(post("/cart/clear").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void checkoutClearsCartAndReservationAndRejectsDuplicateTokenResubmit() throws Exception {
        User buyer = createBuyer();
        Product product = createProduct(1999, 5);
        MockHttpSession session = new MockHttpSession();

        // 1. Add product to cart
        mockMvc.perform(post("/cart/add/" + product.getReference()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("added", true));
        String reservationId = stockReservationService.getReservationId(session);
        assertThat(reservationId).isNotBlank();

        // 2. GET /order generates checkoutToken and renders totalAmount
        var orderPage = mockMvc.perform(get("/order").session(session).with(user(buyer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"checkoutToken\"")))
                .andExpect(content().string(containsString("19,99 €")))
                .andReturn();

        String checkoutToken = (String) session.getAttribute(OrderController.CHECKOUT_TOKEN_SESSION_KEY);
        assertThat(checkoutToken).isNotBlank();

        long orderCountBefore = orderRepository.count();

        // 3. First POST /order/confirm succeeds
        var confirmResult = mockMvc.perform(post("/order/confirm").session(session).with(user(buyer)).with(csrf())
                        .param("checkoutToken", checkoutToken)
                        .param("paymentMethod", PaymentMethod.CREDIT_CARD.name())
                        .param("fullName", buyer.getFullName())
                        .param("address", buyer.getAddress()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/user/dashboard/orders/")))
                .andExpect(flash().attribute("orderSuccess", true))
                .andReturn();

        // Must NOT flash the Order JPA entity
        assertThat(confirmResult.getFlashMap().values())
                .noneMatch(v -> v instanceof Order);

        // Cart cleared and reservation attributes removed from session
        SessionCart cartAfter = (SessionCart) session.getAttribute(CartService.CART_SESSION_KEY);
        assertThat(cartAfter.getProducts()).isEmpty();
        assertThat(session.getAttribute(StockReservationService.RESERVATION_ID_KEY)).isNull();
        assertThat(session.getAttribute(StockReservationService.RESERVATION_TIME_KEY)).isNull();
        assertThat(session.getAttribute(OrderController.CHECKOUT_TOKEN_SESSION_KEY)).isNull();
        assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(4);

        // 4. Duplicate resubmit with the SAME checkoutToken must be rejected and create no second order
        mockMvc.perform(post("/order/confirm").session(session).with(user(buyer)).with(csrf())
                        .param("checkoutToken", checkoutToken)
                        .param("paymentMethod", PaymentMethod.CREDIT_CARD.name())
                        .param("fullName", buyer.getFullName())
                        .param("address", buyer.getAddress()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
    }

    @Test
    void checkoutRejectsMissingPaymentMethodWith400() throws Exception {
        User buyer = createBuyer();
        Product product = createProduct(1500, 3);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add/" + product.getReference()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/order").session(session).with(user(buyer)))
                .andExpect(status().isOk());
        String checkoutToken = (String) session.getAttribute(OrderController.CHECKOUT_TOKEN_SESSION_KEY);

        long ordersBefore = orderRepository.count();

        // Missing paymentMethod without Referer returns 400 Bad Request
        mockMvc.perform(post("/order/confirm").session(session).with(user(buyer)).with(csrf())
                        .param("checkoutToken", checkoutToken)
                        .param("fullName", buyer.getFullName())
                        .param("address", buyer.getAddress()))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isEqualTo(ordersBefore);

        // Cleanup reservation
        mockMvc.perform(post("/cart/clear").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void sessionCartIsThreadSafeAndReturnsDefensiveUnmodifiableCopies() throws Exception {
        SessionCart cart = new SessionCart();
        cart.deleteOneProduct("ABSENT");
        assertThat(cart.getProducts()).isEmpty();

        cart.addProduct("REF-1");
        Map<String, Integer> snapshot = cart.getProducts();
        assertThatThrownBy(() -> snapshot.put("REF-2", 1))
                .isInstanceOf(UnsupportedOperationException.class);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> writer1 = pool.submit(() -> {
                await(start);
                for (int i = 0; i < 200; i++) {
                    cart.addProduct("P-" + (i % 10));
                }
            });
            Future<?> writer2 = pool.submit(() -> {
                await(start);
                for (int i = 0; i < 200; i++) {
                    cart.deleteOneProduct("P-" + (i % 10));
                }
            });
            Future<?> reader = pool.submit(() -> {
                await(start);
                for (int i = 0; i < 200; i++) {
                    Map<String, Integer> copy = cart.getProducts();
                    for (Map.Entry<String, Integer> e : copy.entrySet()) {
                        assertThat(e.getValue()).isPositive();
                    }
                }
            });
            start.countDown();
            for (Future<?> f : List.of(writer1, writer2, reader)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void anonymousCartReservationSurvivesLoginSessionIdRotationAndCompletesCheckout() throws Exception {
        User buyer = createBuyer();
        Product product = createProduct(1650, 3);
        MockHttpSession session = new MockHttpSession();
        String sessionIdBeforeLogin = session.getId();

        // Anonymous user reserves 1 unit in cart
        mockMvc.perform(post("/cart/add/" + product.getReference()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("added", true));
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(2);

        // User logs in -> session ID rotates (changeSessionId) without dropping reservation
        var loginResult = mockMvc.perform(post("/auth").session(session).with(csrf())
                        .param("username", buyer.getUsername())
                        .param("password", "StrongPassword9!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessionAfterLogin = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(sessionAfterLogin).isNotNull();
        assertThat(sessionAfterLogin.getId()).isNotEqualTo(sessionIdBeforeLogin);
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(2);

        // User proceeds directly to checkout with preserved cart and reservation
        mockMvc.perform(get("/order").session(sessionAfterLogin))
                .andExpect(status().isOk());
        String checkoutToken = (String) sessionAfterLogin.getAttribute(OrderController.CHECKOUT_TOKEN_SESSION_KEY);
        assertThat(checkoutToken).isNotBlank();

        mockMvc.perform(post("/order/confirm").session(sessionAfterLogin).with(csrf())
                        .param("checkoutToken", checkoutToken)
                        .param("paymentMethod", PaymentMethod.PAYPAL.name())
                        .param("fullName", buyer.getFullName())
                        .param("address", buyer.getAddress()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/user/dashboard/orders/")));

        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(2);
    }
}
