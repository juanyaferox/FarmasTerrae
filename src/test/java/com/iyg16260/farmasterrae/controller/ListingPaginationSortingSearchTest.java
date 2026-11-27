package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import com.iyg16260.farmasterrae.spec.OrderSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Listings driven by request parameters: page numbers are clamped to the pages that exist, sort columns are
 * allowlisted, and searches neither duplicate rows nor treat {@code %}/{@code _} or blank input specially.
 */
@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:listingpaginationtestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class ListingPaginationSortingSearchTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private ProductsService productsService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;

    // ------------------------------------------------------------------ pagination

    @ParameterizedTest
    @CsvSource ({
            "/products,                107374182, products",
            "/products,                2147483647, products",
            "/admin/dashboard/products, 9999999,  products",
            "/admin/dashboard/orders,   9999999,  orders",
            "/admin/dashboard/users,    9999999,  users"
    })
    void hugePageNumberIsClampedToTheLastPage(String path, String page, String attribute) throws Exception {
        User admin = createUser("ADMIN");

        Page<?> clamped = pageOf(get(path).param("page", page).with(user(admin)), attribute);
        Page<?> last = pageOf(get(path).param("page", String.valueOf(clamped.getTotalPages() - 1)).with(user(admin)), attribute);

        assertThat(clamped.getTotalPages()).isPositive();
        assertThat(clamped.getNumber()).isEqualTo(clamped.getTotalPages() - 1);
        assertThat(clamped.getContent()).isNotEmpty().isEqualTo(last.getContent());
    }

    @ParameterizedTest
    @CsvSource ({
            "/products,                 products",
            "/admin/dashboard/products, products",
            "/admin/dashboard/orders,   orders",
            "/admin/dashboard/users,    users"
    })
    void negativePageNumberIsTheFirstPage(String path, String attribute) throws Exception {
        User admin = createUser("ADMIN");

        Page<?> negative = pageOf(get(path).param("page", "-5").with(user(admin)), attribute);
        Page<?> first = pageOf(get(path).with(user(admin)), attribute);

        assertThat(negative.getNumber()).isZero();
        assertThat(negative.getContent()).isNotEmpty().isEqualTo(first.getContent());
    }

    @Test
    void customerOrdersAndReviewsClampOutOfRangePages() throws Exception {
        User customer = createUser("USER");
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null).limit(11).toList();
        for (int i = 0; i < 10; i++) {
            createOrder(customer, List.of(products.get(i)), SaleStatus.PENDING, PaymentMethod.CREDIT_CARD, "Calle " + i);
        }
        for (Product product : products) {
            createReview(customer, product);
        }

        // 10 orders in pages of 9 and 11 reviews in pages of 10: both end on page index 1
        Page<?> lastOrders = pageOf(get("/user/dashboard/orders").param("page", "9999999").with(user(customer)), "orders");
        assertThat(lastOrders.getNumber()).isEqualTo(1);
        assertThat(lastOrders.getContent()).hasSize(1);
        Page<?> firstOrders = pageOf(get("/user/dashboard/orders").param("page", "-5").with(user(customer)), "orders");
        assertThat(firstOrders.getNumber()).isZero();
        assertThat(firstOrders.getContent()).hasSize(9);

        Page<?> lastReviews = pageOf(get("/user/dashboard/reviews").param("page", "9999999").with(user(customer)), "reviews");
        assertThat(lastReviews.getNumber()).isEqualTo(1);
        assertThat(lastReviews.getContent()).hasSize(1);
        Page<?> firstReviews = pageOf(get("/user/dashboard/reviews").param("page", "-5").with(user(customer)), "reviews");
        assertThat(firstReviews.getNumber()).isZero();
        assertThat(firstReviews.getContent()).hasSize(10);
    }

    @Test
    void outOfRangePageOfAnEmptyListingIsTheFirstPage() throws Exception {
        User customer = createUser("USER");

        Page<?> orders = pageOf(get("/user/dashboard/orders").param("page", "9999999").with(user(customer)), "orders");
        Page<?> reviews = pageOf(get("/user/dashboard/reviews").param("page", "9999999").with(user(customer)), "reviews");
        Page<?> products = pageOf(get("/products").param("category", "BABY").param("page", "9999999"), "products");

        for (Page<?> page : List.of(orders, reviews, products)) {
            assertThat(page.getNumber()).isZero();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    // ------------------------------------------------------------------ sorting

    @ParameterizedTest
    @CsvSource ({
            "/admin/dashboard/products, products, nonExistentField",
            "/admin/dashboard/products, products, password",
            "/admin/dashboard/products, products, userModified.password",
            "/admin/dashboard/orders,   orders,   nonExistentField",
            "/admin/dashboard/orders,   orders,   user.password",
            "/admin/dashboard/users,    users,    password",
            "/admin/dashboard/users,    users,    profile.type",
            "/user/dashboard/orders,    orders,   user.password",
            "/user/dashboard/orders,    orders,   nonExistentField",
            "/user/dashboard/reviews,   reviews,  user.password",
            "/user/dashboard/reviews,   reviews,  password"
    })
    void unknownOrSensitiveSortColumnsFallBackToTheDefaultOrder(String path, String attribute, String sort) throws Exception {
        // An admin with orders and reviews of their own, so every listing has rows to order
        User admin = createUser("ADMIN");
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null).limit(3).toList();
        for (Product product : products) {
            createOrder(admin, List.of(product), SaleStatus.PENDING, PaymentMethod.CREDIT_CARD, "Calle Orden");
            createReview(admin, product);
        }

        // Neither a server error nor an order by a column the table does not offer (e.g. a password hash)
        Page<?> requested = pageOf(get(path).param("sort", sort).param("dir", "desc").with(user(admin)), attribute);
        Page<?> unsorted = pageOf(get(path).with(user(admin)), attribute);

        assertThat(requested.getContent()).hasSizeGreaterThan(1).isEqualTo(unsorted.getContent());
    }

    @ParameterizedTest
    @CsvSource ({
            "/admin/dashboard/products, name", "/admin/dashboard/products, reference",
            "/admin/dashboard/products, category", "/admin/dashboard/products, price",
            "/admin/dashboard/products, stock",
            "/admin/dashboard/orders, id", "/admin/dashboard/orders, createdAt",
            "/admin/dashboard/orders, totalPrice", "/admin/dashboard/orders, updatedAt",
            "/admin/dashboard/orders, status",
            "/admin/dashboard/users, username", "/admin/dashboard/users, email",
            "/admin/dashboard/users, phone", "/admin/dashboard/users, address",
            "/user/dashboard/orders, id", "/user/dashboard/orders, createdAt",
            "/user/dashboard/orders, totalPrice", "/user/dashboard/orders, updatedAt",
            "/user/dashboard/orders, status",
            "/user/dashboard/reviews, title", "/user/dashboard/reviews, content",
            "/user/dashboard/reviews, scoring"
    })
    void everySortableColumnOfTheTablesStillWorks(String path, String sort) throws Exception {
        User admin = createUser("ADMIN");

        for (String dir : List.of("asc", "desc")) {
            mockMvc.perform(get(path).param("sort", sort).param("dir", dir).with(user(admin)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void allowedSortsAreAppliedAndDirectionIsCaseInsensitive() {
        List<BigDecimal> prices = productsService.getProductList(0, null, "price", "DESC").getContent().stream()
                .map(ProductDTO::getPrice).toList();
        assertThat(prices).isSortedAccordingTo(Comparator.reverseOrder());

        List<BigDecimal> totals = orderService.getAllOrders(0, null, "totalPrice", "Desc").getContent().stream()
                .map(OrderDTO::getTotalPrice).toList();
        assertThat(totals).isSortedAccordingTo(Comparator.reverseOrder());

        List<String> usernames = userService.getAllUsers(0, null, "username", "asc").getContent().stream()
                .map(UserDTO::getUsername).toList();
        assertThat(usernames).isSorted();
    }

    @Test
    void invalidSortUsesTheSameOrderAsNoSort() {
        assertThat(productsService.getProductList(0, null, "password", "desc").getContent())
                .isEqualTo(productsService.getProductList(0, null, null, null).getContent());
        assertThat(orderService.getAllOrders(0, null, "user.password", "desc").getContent())
                .extracting(OrderDTO::getId)
                .isEqualTo(orderService.getAllOrders(0, null, null, null).getContent().stream().map(OrderDTO::getId).toList())
                .isSorted();
    }

    // ------------------------------------------------------------------ search

    @Test
    void orderMatchingSeveralOfItsLinesIsListedExactlyOnce() {
        String keyword = "Multilinea" + suffix();
        User buyer = createUser("USER");
        List<Product> lines = List.of(
                createProduct(keyword + " uno", "Desc " + keyword),
                createProduct(keyword + " dos", "Desc " + keyword),
                createProduct(keyword + " tres", "Desc " + keyword));
        Order order = createOrder(buyer, lines, SaleStatus.PENDING, PaymentMethod.TRANSFER, "Calle Larga 3");

        // The count query counts the order once, not once per joined row (3 lines x 3 searched product fields)
        assertThat(orderRepository.count(new OrderSpecification().searchLike(keyword))).isEqualTo(1);
        // Asking for the next page does not return the same order again
        Page<OrderDTO> next = orderService.getAllOrders(1, keyword, null, null);
        assertThat(next.getNumber()).isZero();
        assertThat(next.getTotalElements()).isEqualTo(1);
        assertThat(next.getContent()).extracting(OrderDTO::getId).containsExactly(order.getId());

        for (String sort : new String[]{null, "createdAt", "totalPrice", "status"}) {
            Page<OrderDTO> result = orderService.getAllOrders(0, keyword, sort, "desc");

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent()).extracting(OrderDTO::getId).containsExactly(order.getId());
        }
        // The shipping name/address and the products are searched in the same query
        assertThat(orderService.getAllOrders(0, "Calle Larga 3", null, null).getContent())
                .extracting(OrderDTO::getId).containsExactly(order.getId());
    }

    @Test
    void severalMultiLineOrdersArePagedWithoutLosingOrRepeatingAny() {
        String keyword = "Paginado" + suffix();
        User buyer = createUser("USER");
        List<Product> lines = List.of(
                createProduct(keyword + " a", "Desc"),
                createProduct(keyword + " b", "Desc"),
                createProduct(keyword + " c", "Desc"));
        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            expected.add(createOrder(buyer, lines, SaleStatus.SHIPPED, PaymentMethod.CREDIT_CARD, "Calle " + i).getId());
        }

        // 12 orders in pages of 9: the row limit applies to orders, not to the rows of the joined lines
        Page<OrderDTO> first = orderService.getAllOrders(0, keyword, "id", "asc");
        assertThat(first.getTotalElements()).isEqualTo(12);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(OrderDTO::getId).containsExactlyElementsOf(expected.subList(0, 9));
        assertThat(allPages(page -> orderService.getAllOrders(page, keyword, "id", "asc")))
                .extracting(OrderDTO::getId).containsExactlyElementsOf(expected);
    }

    @Test
    void nestedSearchFieldsShareOneJoinPerPathAndMakeTheQueryDistinct() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);

        new OrderSpecification().searchLike("ibuprofeno").toPredicate(root, query, cb);

        assertThat(root.getJoins()).singleElement().satisfies(orderDetails -> {
            assertThat(orderDetails.getAttribute().getName()).isEqualTo("orderDetails");
            assertThat(orderDetails.getJoins()).extracting(join -> join.getAttribute().getName())
                    .containsExactly("product");
        });
        assertThat(query.isDistinct()).isTrue();
    }

    @Test
    void likeWildcardsInTheSearchAreMatchedLiterally() {
        String marker = suffix();
        Product underscore = createProduct("Comodin " + marker + "_9", "Desc");
        Product notUnderscore = createProduct("Comodin " + marker + "X9", "Desc");
        Product percent = createProduct("Rebaja " + marker + "%9", "Desc");
        Product notPercent = createProduct("Rebaja " + marker + "AB9", "Desc");

        assertThat(adminProductReferences(marker + "_9")).containsExactly(underscore.getReference());
        assertThat(adminProductReferences(marker + "%9")).containsExactly(percent.getReference());
        assertThat(adminProductReferences(marker + "\\")).isEmpty();
        assertThat(productsService.getFilteredProducts(null, marker + "_9", 0).getContent())
                .extracting(p -> p.getReference()).containsExactly(underscore.getReference());
        // A bare wildcard only matches values that really contain it
        assertThat(productsService.getProductList(0, "%", null, null).getContent())
                .extracting(ProductDTO::getReference).contains(percent.getReference())
                .doesNotContain(notUnderscore.getReference(), notPercent.getReference(), underscore.getReference());

        User buyer = createUser("USER");
        Order discounted = createOrder(buyer, List.of(notPercent), SaleStatus.PENDING, PaymentMethod.PAYPAL, "Oferta " + marker + "%");
        Order plain = createOrder(buyer, List.of(notPercent), SaleStatus.PENDING, PaymentMethod.PAYPAL, "Oferta " + marker + "0");
        assertThat(orderService.getAllOrders(0, marker + "%", null, null).getContent())
                .extracting(OrderDTO::getId).containsExactly(discounted.getId()).doesNotContain(plain.getId());

        User snake = createUser("USER", "w" + marker + "_z");
        createUser("USER", "w" + marker + "kz");
        assertThat(userService.getAllUsers(0, marker + "_z", null, null).getContent())
                .extracting(UserDTO::getUsername).containsExactly(snake.getUsername());
    }

    @ParameterizedTest
    @ValueSource (strings = {" ", "   ", "\t"})
    void whitespaceOnlySearchBehavesLikeNoSearch(String blank) throws Exception {
        assertThat(productsService.getProductList(0, blank, null, null).getTotalElements())
                .isEqualTo(productsService.getProductList(0, null, null, null).getTotalElements());
        assertThat(productsService.getFilteredProducts(null, blank, 0).getTotalElements())
                .isEqualTo(productsService.getFilteredProducts(null, null, 0).getTotalElements());
        // Single-word labels ("Medicamentos", "Paypal") used to be left out when a blank search was OR'ed
        // with the enum labels that contain a space
        assertThat(allPages(page -> productsService.getProductList(page, blank, null, null)))
                .extracting(ProductDTO::getCategory).contains(Category.MEDICINE);

        Page<OrderDTO> orders = orderService.getAllOrders(0, blank, null, null);
        assertThat(orders.getTotalElements()).isEqualTo(orderRepository.count());
        assertThat(allPages(page -> orderService.getAllOrders(page, blank, null, null)))
                .extracting(OrderDTO::getId).containsAll(paypalOrderIds());

        assertThat(userService.getAllUsers(0, blank, null, null).getTotalElements())
                .isEqualTo(userService.getAllUsers(0, null, null, null).getTotalElements());

        MvcResult storefront = mockMvc.perform(get("/products").param("search", blank))
                .andExpect(status().isOk()).andReturn();
        assertThat(storefront.getModelAndView().getModel().get("text")).isEqualTo("Todos los Productos");
    }

    @Test
    void searchKeywordsAreTrimmed() {
        Product product = createProduct("Recortable " + suffix(), "Desc");

        assertThat(adminProductReferences("   " + product.getName() + "  ")).containsExactly(product.getReference());
        List<Long> paypal = paypalOrderIds();
        assertThat(paypal).isNotEmpty();
        assertThat(allPages(page -> orderService.getAllOrders(page, "  Paypal ", null, null)))
                .extracting(OrderDTO::getId).containsExactlyInAnyOrderElementsOf(paypal);
    }

    // ------------------------------------------------------------------ helpers

    private Page<?> pageOf(MockHttpServletRequestBuilder request, String attribute) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        Object page = result.getModelAndView().getModel().get(attribute);
        assertThat(page).isInstanceOf(Page.class);
        return (Page<?>) page;
    }

    /** Every element of a listing, page by page */
    private static <T> List<T> allPages(Function<Integer, Page<T>> listing) {
        List<T> all = new ArrayList<>();
        Page<T> page;
        int number = 0;
        do {
            page = listing.apply(number++);
            all.addAll(page.getContent());
        } while (page.hasNext());
        return all;
    }

    private List<Long> paypalOrderIds() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getPaymentMethod() == PaymentMethod.PAYPAL)
                .map(Order::getId)
                .toList();
    }

    private List<String> adminProductReferences(String keyword) {
        return productsService.getProductList(0, keyword, null, null).getContent().stream()
                .map(ProductDTO::getReference).toList();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private User createUser(String roleType) {
        return createUser(roleType, "lst" + suffix());
    }

    private User createUser(String roleType, String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Cliente " + username);
        user.setAddress("Calle Listado 1");
        user.setPassword(passwordEncoder.encode("StrongPassword9!"));
        user.setProfile(profileRepository.findByType(roleType));
        return userRepository.saveAndFlush(user);
    }

    private Product createProduct(String name, String description) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setReference("LST-" + suffix());
        product.setPrice(1000);
        product.setStock(50);
        product.setCategory(Category.CARE);
        return productRepository.saveAndFlush(product);
    }

    private void createReview(User author, Product product) {
        Review review = new Review();
        review.setUser(author);
        review.setProduct(product);
        review.setTitle("Opinión " + product.getReference());
        review.setContent("Contenido");
        review.setScoring(8);
        reviewRepository.saveAndFlush(review);
    }

    private Order createOrder(User buyer, List<Product> products, SaleStatus status, PaymentMethod method, String address) {
        return transactionTemplate.execute(tx -> {
            Order order = new Order();
            order.setUser(userRepository.getReferenceById(buyer.getId()));
            order.setStatus(status);
            order.setPaymentMethod(method);
            order.setTotalPrice(BigDecimal.valueOf(10L * products.size()).setScale(2));
            order.setName(buyer.getFullName());
            order.setAddress(address);
            Order saved = orderRepository.save(order);
            List<OrderDetails> lines = new ArrayList<>();
            for (Product product : products) {
                OrderDetails line = new OrderDetails(saved, productRepository.getReferenceById(product.getId()));
                line.setAmount(1);
                line.setUnitPriceCents(product.getPrice());
                line.setProductName(product.getName());
                lines.add(line);
            }
            saved.setOrderDetails(lines);
            orderRepository.flush();
            return saved;
        });
    }
}
