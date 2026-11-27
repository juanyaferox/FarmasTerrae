package com.iyg16260.farmasterrae.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Frontend supply chain and markup: Alpine.js and the fonts are served by the app itself, the CSP only allows the
 * app's own origin, the Node build files are not public, and the pages render the expected Spanish markup.
 */
@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:frontendassetstestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class FrontendAssetsAndPagesTest {

    static final String EXPECTED_CSP = "default-src 'self'; script-src 'self' 'unsafe-eval'; "
            + "style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self' data: blob: https: http://localhost:9000; "
            + "frame-ancestors 'self'; object-src 'none'; base-uri 'self'; form-action 'self'";

    private static final String SIGNED_URL = "https://signed.example/product-images/thumb.jpg?X-Amz-Expires=600&X-Amz-Signature=abc123";

    private static final List<String> FONTS = List.of(
            "inter-latin.woff2", "inter-latin-ext.woff2",
            "inter-tight-latin.woff2", "inter-tight-latin-ext.woff2",
            "jetbrains-mono-latin.woff2", "jetbrains-mono-latin-ext.woff2");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private S3Presigner s3Presigner;

    @BeforeEach
    void stubPresigner() throws Exception {
        reset(s3Presigner);
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create(SIGNED_URL).toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
    }

    // ------------------------------------------------------------------ self-hosted assets

    @ParameterizedTest
    @ValueSource (strings = {
            "/js/vendor/alpine-3.14.9.min.js",
            "/assets/fonts/inter-latin.woff2", "/assets/fonts/inter-latin-ext.woff2",
            "/assets/fonts/inter-tight-latin.woff2", "/assets/fonts/inter-tight-latin-ext.woff2",
            "/assets/fonts/jetbrains-mono-latin.woff2", "/assets/fonts/jetbrains-mono-latin-ext.woff2",
            "/assets/icons/favicon/site.webmanifest", "/assets/icons/favicon/android-chrome-512x512.png"
    })
    void selfHostedAssetsAreServedAnonymously(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void vendoredAlpineIsThePinnedRelease() throws Exception {
        String script = mockMvc.perform(get("/js/vendor/alpine-3.14.9.min.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(script).contains("version:\"3.14.9\"");
    }

    @Test
    void fontsAreRealWoff2Files() throws Exception {
        for (String font : FONTS) {
            byte[] body = mockMvc.perform(get("/assets/fonts/" + font))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(body, 0, 4, StandardCharsets.US_ASCII)).as(font).isEqualTo("wOF2");
        }
    }

    @Test
    void webManifestNamesTheAppAndUsesRootRelativeIconPaths() throws Exception {
        String body = mockMvc.perform(get("/assets/icons/favicon/site.webmanifest"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode manifest = objectMapper.readTree(body);

        assertThat(manifest.get("name").asText()).isEqualTo("FarmasTerrae");
        assertThat(manifest.get("short_name").asText()).isEqualTo("FarmasTerrae");
        assertThat(manifest.get("start_url").asText()).isEqualTo("/");
        assertThat(manifest.get("icons").findValuesAsText("src")).containsExactly(
                "/assets/icons/favicon/android-chrome-192x192.png",
                "/assets/icons/favicon/android-chrome-512x512.png");
    }

    @ParameterizedTest
    @ValueSource (strings = {"/package.json", "/package-lock.json", "/postcss.config.js", "/dist/output.css", "/css/input.css"})
    void nodeBuildFilesAreNeitherPackagedNorServed(String path) throws Exception {
        assertThat(new ClassPathResource("static" + path).exists()).as(path).isFalse();

        int anonymous = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
        assertThat(anonymous).as(path).isNotEqualTo(200);
        mockMvc.perform(get(path).with(user("signed-in"))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ CSP and page shell

    @Test
    void contentSecurityPolicyOnlyAllowsTheAppsOwnOrigin() throws Exception {
        String csp = mockMvc.perform(get("/auth"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", EXPECTED_CSP))
                .andReturn().getResponse().getHeader("Content-Security-Policy");

        assertThat(csp).doesNotContain("cdn.jsdelivr.net", "fonts.googleapis.com", "fonts.gstatic.com");
    }

    @ParameterizedTest
    @ValueSource (strings = {"/", "/products", "/products/categories", "/products/REFMED001", "/cart", "/auth",
            "/auth/register", "/auth/forgotPassword", "/products/does-not-exist"})
    void pagesAreSpanishAndOnlyLoadLocalScripts(String path) throws Exception {
        String html = mockMvc.perform(get(path).header("Accept-Language", "en-US,en;q=0.9"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("<html lang=\"es\"")
                .doesNotContain("lang=\"en\"")
                .contains("src=\"/js/vendor/alpine-3.14.9.min.js\"")
                .doesNotContain("cdn.jsdelivr.net")
                .doesNotContain("fonts.googleapis.com")
                .doesNotContain("fonts.gstatic.com")
                .doesNotContain("/dist/output.css");
    }

    @Test
    void dashboardsAreSpanishToo() throws Exception {
        User admin = createUser("ADMIN");
        User customer = createUser("USER");

        for (String path : List.of("/admin/dashboard", "/admin/dashboard/products", "/admin/dashboard/orders",
                "/admin/dashboard/users")) {
            assertThat(html(get(path).with(user(admin)).locale(Locale.ENGLISH))).contains("<html lang=\"es\"");
        }
        for (String path : List.of("/user/dashboard", "/user/dashboard/orders", "/user/dashboard/reviews",
                "/user/dashboard/info-user")) {
            assertThat(html(get(path).with(user(customer)).locale(Locale.ENGLISH))).contains("<html lang=\"es\"");
        }
    }

    @Test
    void tokensStylesheetUsesTheSelfHostedFonts() throws Exception {
        String css = mockMvc.perform(get("/css/tokens.css"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(css).doesNotContain("fonts.googleapis.com")
                .doesNotContain("fonts.gstatic.com")
                .doesNotContain("@import")
                .contains("font-display: swap");
        for (String font : FONTS) {
            assertThat(css).contains("/assets/fonts/" + font);
        }
        // Every font the stylesheet points to is really served (paths are relative to /css/tokens.css)
        Matcher url = Pattern.compile("url\\('\\.\\./([^']+)'\\)").matcher(css);
        int faces = 0;
        while (url.find()) {
            mockMvc.perform(get("/" + url.group(1))).andExpect(status().isOk());
            faces++;
        }
        assertThat(faces).isEqualTo(FONTS.size());
    }

    // ------------------------------------------------------------------ copy and markup

    @Test
    void paymentCopyMakesNoAbsoluteSecurityPromise() throws Exception {
        for (String path : List.of("/", "/products", "/cart")) {
            assertThat(html(get(path))).as(path)
                    .contains("Pago protegido")
                    .doesNotContain("100%")
                    .doesNotContain("Pagos seguros");
        }
    }

    @Test
    void activeCategoryChipIsMarkedAsTheCurrentPage() throws Exception {
        String html = html(get("/products").param("category", "MEDICINE"));

        assertThat(html).doesNotContain("aria-pressed");
        assertThat(html).containsPattern("<a [^>]*aria-current=\"page\"[^>]*>Medicamentos</a>");
        assertThat(chip(html, "Todos")).doesNotContain("aria-current");
        assertThat(chip(html(get("/products")), "Todos")).contains("aria-current=\"page\"");
    }

    @Test
    void paginationOnlyShowsTheEmptyMessageWhenThereAreNoResults() throws Exception {
        // CARE: 10 products, a single page
        String singlePage = html(get("/products").param("category", "CARE"));
        assertThat(singlePage).doesNotContain("ft-pagination__empty")
                .doesNotContain("No hay más")
                .doesNotContain("aria-label=\"Paginación\"");

        // BABY: no products at all
        assertThat(html(get("/products").param("category", "BABY")))
                .contains("No hay productos disponibles para mostrar.");

        // The whole catalogue spans several pages
        assertThat(html(get("/products"))).contains("aria-label=\"Paginación\"").doesNotContain("ft-pagination__empty");
    }

    @Test
    void hugePageStillRendersAShortPaginationBar() throws Exception {
        String html = html(get("/products").param("page", "107374182"));

        Matcher info = Pattern.compile("Página (\\d+) de (\\d+)").matcher(html);
        assertThat(info.find()).isTrue();
        assertThat(info.group(1)).isEqualTo(info.group(2));
        assertThat(html).doesNotContain("value=\"107374181\"").doesNotContain(">Siguiente</button>");
        assertThat(Pattern.compile("name=\"page\"").matcher(html).results().count()).isLessThan(10);
    }

    @Test
    void adminProductThumbnailsKeepSignedUrlsAndUseThePlaceholderOtherwise() throws Exception {
        User admin = createUser("ADMIN");
        Product withImage = createProduct("product-images/thumb-" + suffix() + ".jpg");
        Product withoutImage = createProduct(null);

        assertThat(html(get("/admin/dashboard/products").param("search", withImage.getReference()).with(user(admin))))
                .contains("src=\"" + SIGNED_URL.replace("&", "&amp;") + "\"");
        assertThat(html(get("/admin/dashboard/products").param("search", withoutImage.getReference()).with(user(admin))))
                .contains("src=\"/assets/img/placeholder.svg\"");
    }

    // ------------------------------------------------------------------ order details: admin vs customer

    @Test
    void adminOrderDetailsUseTheAdminNavigationAndHideCustomerActions() throws Exception {
        User admin = createUser("ADMIN");
        User customer = createUser("USER");
        Order order = createOrder(customer, createProduct(null));

        String html = html(get("/admin/dashboard/orders/" + order.getId()).with(user(admin)));

        assertThat(section(html, "class=\"ft-breadcrumb\"", "</nav>"))
                .contains("href=\"/admin/dashboard\"")
                .contains("href=\"/admin/dashboard/orders\"")
                .doesNotContain("/user/dashboard");
        assertThat(section(html, "class=\"ud-actions-bar\"", "</div>"))
                .contains("href=\"/admin/dashboard/orders\"")
                .doesNotContain("/user/dashboard")
                .doesNotContain("Escribir reseña");
    }

    @Test
    void customerOrderDetailsKeepTheCustomerNavigation() throws Exception {
        User customer = createUser("USER");
        Order order = createOrder(customer, createProduct(null));

        String html = html(get("/user/dashboard/orders/" + order.getId()).with(user(customer)));

        assertThat(section(html, "class=\"ft-breadcrumb\"", "</nav>"))
                .contains("href=\"/user/dashboard\"")
                .contains("href=\"/user/dashboard/orders\"")
                .doesNotContain("/admin/dashboard");
        assertThat(section(html, "class=\"ud-actions-bar\"", "</div>"))
                .contains("href=\"/user/dashboard/orders\"")
                .contains("href=\"/user/dashboard/reviews\"")
                .doesNotContain("/admin/dashboard");
    }

    // ------------------------------------------------------------------ helpers

    private String html(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String section(String html, String start, String end) {
        int from = html.indexOf(start);
        assertThat(from).as("'%s' in page", start).isNotNegative();
        int to = html.indexOf(end, from);
        assertThat(to).as("'%s' after '%s'", end, start).isPositive();
        return html.substring(from, to);
    }

    private static String chip(String html, String label) {
        Matcher chip = Pattern.compile("<a [^>]*class=\"ft-chip[^\"]*\"[^>]*>" + label + "</a>").matcher(html);
        assertThat(chip.find()).as("chip %s", label).isTrue();
        return chip.group();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private User createUser(String roleType) {
        String username = "fe" + suffix();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Usuaria " + username);
        user.setAddress("Calle Front 1");
        user.setPassword(passwordEncoder.encode("StrongPassword9!"));
        user.setProfile(profileRepository.findByType(roleType));
        return userRepository.saveAndFlush(user);
    }

    private Product createProduct(String imageKey) {
        Product product = new Product();
        product.setReference("FE-" + suffix());
        product.setName("Producto " + product.getReference());
        product.setDescription("Desc");
        product.setPrice(1999);
        product.setStock(5);
        // Not CARE/BABY: the pagination test relies on how many products those categories have
        product.setCategory(Category.BEAUTY);
        product.setImagePath(imageKey);
        return productRepository.saveAndFlush(product);
    }

    private Order createOrder(User buyer, Product product) {
        return transactionTemplate.execute(tx -> {
            Order order = new Order();
            order.setUser(userRepository.getReferenceById(buyer.getId()));
            order.setStatus(SaleStatus.COMPLETED);
            order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            order.setTotalPrice(new BigDecimal("19.99"));
            order.setName(buyer.getFullName());
            order.setAddress(buyer.getAddress());
            Order saved = orderRepository.save(order);
            OrderDetails line = new OrderDetails(saved, productRepository.getReferenceById(product.getId()));
            line.setAmount(1);
            line.setUnitPriceCents(product.getPrice());
            line.setProductName(product.getName());
            saved.setOrderDetails(List.of(line));
            orderRepository.flush();
            return saved;
        });
    }
}
