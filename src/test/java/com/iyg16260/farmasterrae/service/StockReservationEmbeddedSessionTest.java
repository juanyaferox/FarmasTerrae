package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest (
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:issue10embeddedsessiontestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE"
)
class StockReservationEmbeddedSessionTest {

    private static final Pattern CSRF_NAME_FIRST = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
    private static final Pattern CSRF_VALUE_FIRST = Pattern.compile("<input[^>]*value=\"([^\"]+)\"[^>]*name=\"_csrf\"");
    private static final Pattern CHECKOUT_TOKEN_NAME_FIRST = Pattern.compile("<input[^>]*name=\"checkoutToken\"[^>]*value=\"([^\"]+)\"");
    private static final Pattern CHECKOUT_TOKEN_VALUE_FIRST = Pattern.compile("<input[^>]*value=\"([^\"]+)\"[^>]*name=\"checkoutToken\"");

    @LocalServerPort
    private int port;

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

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private User createBuyer() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User buyer = new User();
        buyer.setUsername("emb_" + suffix);
        buyer.setEmail("emb_" + suffix + "@example.com");
        buyer.setFullName("Embedded Buyer " + suffix);
        buyer.setAddress("Calle Real 1");
        buyer.setPassword(passwordEncoder.encode("StrongPassword9!"));
        buyer.setProfile(profileRepository.findByType("USER"));
        return userRepository.saveAndFlush(buyer);
    }

    private Product createProduct(int priceCents, int stock) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Product product = new Product();
        product.setName("Embedded Test Med " + suffix);
        product.setDescription("Desc");
        product.setPrice(priceCents);
        product.setStock(stock);
        product.setReference("EMB-" + UUID.randomUUID().toString().substring(0, 8));
        product.setCategory(Category.MEDICINE);
        return productRepository.saveAndFlush(product);
    }

    private static String extractHiddenInput(String html, Pattern nameFirst, Pattern valueFirst, String name) {
        Matcher m1 = nameFirst.matcher(html);
        if (m1.find()) {
            return m1.group(1);
        }
        Matcher m2 = valueFirst.matcher(html);
        if (m2.find()) {
            return m2.group(1);
        }
        throw new AssertionError("No " + name + " input found in HTML response");
    }

    private static String extractCsrf(String html) {
        return extractHiddenInput(html, CSRF_NAME_FIRST, CSRF_VALUE_FIRST, "_csrf");
    }

    private static String extractCheckoutToken(String html) {
        return extractHiddenInput(html, CHECKOUT_TOKEN_NAME_FIRST, CHECKOUT_TOKEN_VALUE_FIRST, "checkoutToken");
    }

    private static String currentSessionCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> "JSESSIONID".equalsIgnoreCase(c.getName()))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String formEncode(String... keyValues) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(keyValues[i], StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(keyValues[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private HttpClient newClient(CookieManager cookies) {
        return HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private void login(HttpClient client, User buyer) throws Exception {
        HttpResponse<String> loginPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/auth"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);
        String loginCsrf = extractCsrf(loginPage.body());

        HttpResponse<String> loginRes = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/auth")))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formEncode(
                                "_csrf", loginCsrf,
                                "username", buyer.getUsername(),
                                "password", "StrongPassword9!")))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginRes.statusCode()).isEqualTo(302);
    }

    @Test
    void realContainerPreservesReservationAcrossLoginRotationAndReleasesOnSessionInvalidation() throws Exception {
        User buyer = createBuyer();
        Product product = createProduct(1400, 4);

        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = newClient(cookies);

        // 1. Anonymous user visits homepage and product page to obtain CSRF token & session
        //    (verifies deferred CSRF token resolves before Tomcat flushes the streaming Thymeleaf buffer)
        HttpResponse<String> homePage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(homePage.statusCode()).isEqualTo(200);
        assertThat(extractCsrf(homePage.body())).isNotBlank();

        HttpResponse<String> productPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/products/" + product.getReference()))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(productPage.statusCode()).isEqualTo(200);
        String anonCsrf = extractCsrf(productPage.body());

        // 2. Anonymous user adds 1 unit to cart -> real session holds active stock reservation
        HttpResponse<String> addRes = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/cart/add/" + product.getReference())))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formEncode("_csrf", anonCsrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(addRes.statusCode()).isEqualTo(302);

        String jsessionBeforeLogin = currentSessionCookie(cookies);
        assertThat(jsessionBeforeLogin).isNotBlank();
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(3);

        // 3. Anonymous user logs in -> Tomcat rotates JSESSIONID (session fixation protection)
        //    AND active cart reservation stays intact!
        login(client, buyer);

        String jsessionAfterLogin = currentSessionCookie(cookies);
        assertThat(jsessionAfterLogin).isNotBlank().isNotEqualTo(jsessionBeforeLogin);
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(3);

        // 4. Authenticated user logs out -> Tomcat invalidates HttpSession and invokes
        //    StockReservationSessionListener.sessionDestroyed -> reservation released immediately!
        HttpResponse<String> dashboardPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/user/dashboard"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(dashboardPage.statusCode()).isEqualTo(200);
        String logoutCsrf = extractCsrf(dashboardPage.body());

        HttpResponse<String> logoutRes = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/logout")))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formEncode("_csrf", logoutCsrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(logoutRes.statusCode()).isEqualTo(302);
        assertThat(logoutRes.headers().firstValue("Location")).hasValueSatisfying(loc -> assertThat(loc).endsWith("/auth?logout"));

        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(4);
    }

    @Test
    void concurrentConfirmRequestsWithSameCheckoutTokenCreateExactlyOneOrder() throws Exception {
        User buyer = createBuyer();
        Product product = createProduct(1299, 5);

        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = newClient(cookies);
        login(client, buyer);

        HttpResponse<String> productPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/products/" + product.getReference()))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(productPage.statusCode()).isEqualTo(200);
        String csrf = extractCsrf(productPage.body());

        HttpResponse<String> addRes = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/cart/add/" + product.getReference())))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formEncode("_csrf", csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(addRes.statusCode()).isEqualTo(302);

        HttpResponse<String> orderPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl("/order"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(orderPage.statusCode()).isEqualTo(200);
        String checkoutToken = extractCheckoutToken(orderPage.body());
        String orderCsrf = extractCsrf(orderPage.body());

        long ordersBefore = orderRepository.count();
        int threads = 4;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    HttpResponse<String> res = client.send(
                            HttpRequest.newBuilder(URI.create(baseUrl("/order/confirm")))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(
                                            "_csrf", orderCsrf,
                                            "checkoutToken", checkoutToken,
                                            "paymentMethod", PaymentMethod.CREDIT_CARD.name(),
                                            "fullName", buyer.getFullName(),
                                            "address", buyer.getAddress())))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
                    assertThat(res.statusCode()).isEqualTo(302);
                    return res.headers().firstValue("Location").orElse("");
                });
            }

            List<Future<String>> futures = pool.invokeAll(tasks);
            List<String> locations = new ArrayList<>();
            for (Future<String> future : futures) {
                locations.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(locations.stream().filter(loc -> loc.contains("/user/dashboard/orders/")).count()).isEqualTo(1);
            assertThat(locations.stream().filter(loc -> loc.endsWith("/cart")).count()).isEqualTo(threads - 1);
            assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
            assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(4);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentCartClearDuringCheckoutAlwaysEndsInConsistentState() throws Exception {
        User buyer = createBuyer();
        Product product = createProduct(999, 10);

        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = newClient(cookies);
        login(client, buyer);

        for (int attempt = 0; attempt < 5; attempt++) {
            int stockBefore = productRepository.findByReference(product.getReference()).orElseThrow().getStock();
            long ordersBefore = orderRepository.count();

            HttpResponse<String> productPage = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl("/products/" + product.getReference()))).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(productPage.statusCode()).isEqualTo(200);
            String addCsrf = extractCsrf(productPage.body());

            HttpResponse<String> addRes = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl("/cart/add/" + product.getReference())))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(formEncode("_csrf", addCsrf)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(addRes.statusCode()).isEqualTo(302);

            HttpResponse<String> orderPage = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl("/order"))).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(orderPage.statusCode()).isEqualTo(200);
            String checkoutToken = extractCheckoutToken(orderPage.body());
            String csrf = extractCsrf(orderPage.body());

            CyclicBarrier barrier = new CyclicBarrier(2);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<String> confirmFuture = pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    HttpResponse<String> res = client.send(
                            HttpRequest.newBuilder(URI.create(baseUrl("/order/confirm")))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(
                                            "_csrf", csrf,
                                            "checkoutToken", checkoutToken,
                                            "paymentMethod", PaymentMethod.CREDIT_CARD.name(),
                                            "fullName", buyer.getFullName(),
                                            "address", buyer.getAddress())))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
                    assertThat(res.statusCode()).isEqualTo(302);
                    return res.headers().firstValue("Location").orElse("");
                });
                Future<Integer> clearFuture = pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    HttpResponse<String> res = client.send(
                            HttpRequest.newBuilder(URI.create(baseUrl("/cart/clear")))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(HttpRequest.BodyPublishers.ofString(formEncode("_csrf", csrf)))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
                    return res.statusCode();
                });

                String confirmLocation = confirmFuture.get(10, TimeUnit.SECONDS);
                assertThat(clearFuture.get(10, TimeUnit.SECONDS)).isEqualTo(302);

                Product reloaded = productRepository.findByReference(product.getReference()).orElseThrow();
                int stockAfter = reloaded.getStock();
                long ordersAfter = orderRepository.count();

                assertThat(stockReservationService.getAvailableStock(reloaded, null)).isEqualTo(stockAfter);

                if (confirmLocation.contains("/user/dashboard/orders/")) {
                    // Checkout held session mutex first: 1 order created, stock decremented by 1
                    assertThat(ordersAfter).isEqualTo(ordersBefore + 1);
                    assertThat(stockAfter).isEqualTo(stockBefore - 1);
                } else {
                    // Clear held session mutex first: 0 orders created, stock untouched
                    assertThat(confirmLocation).endsWith("/cart");
                    assertThat(ordersAfter).isEqualTo(ordersBefore);
                    assertThat(stockAfter).isEqualTo(stockBefore);
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
