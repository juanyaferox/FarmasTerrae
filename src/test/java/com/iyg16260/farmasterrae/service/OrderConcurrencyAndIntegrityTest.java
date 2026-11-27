package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.payment.PaymentDetailsDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.mapper.OrderMapper;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:issue10orderintegritytestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class OrderConcurrencyAndIntegrityTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private ProductsService productsService;

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
    private OrderMapper orderMapper;

    @Autowired
    private MockMvc mockMvc;

    private User createUser(String roleType) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User u = new User();
        u.setUsername("u_" + suffix);
        u.setEmail("u_" + suffix + "@example.com");
        u.setFullName("Test User " + suffix);
        u.setAddress("Calle Test " + suffix);
        u.setPassword(passwordEncoder.encode("StrongPassword9!"));
        u.setProfile(profileRepository.findByType(roleType));
        return userRepository.saveAndFlush(u);
    }

    private Product createProduct(String name, int priceCents, int stock, User modifier) {
        Product p = new Product();
        p.setName(name);
        p.setDescription("Initial description");
        p.setPrice(priceCents);
        p.setStock(stock);
        p.setReference("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        p.setCategory(Category.MEDICINE);
        p.setUserModified(modifier);
        return productRepository.saveAndFlush(p);
    }

    private PaymentDetailsDTO paymentDetails(User u) {
        PaymentDetailsDTO dto = new PaymentDetailsDTO();
        dto.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        dto.setAddress(u.getAddress());
        dto.setFullName(u.getFullName());
        return dto;
    }

    @Test
    void concurrentCheckoutOnStockOneAllowsExactlyOneOrderAndNoOverselling() throws Exception {
        User buyer1 = createUser("USER");
        User buyer2 = createUser("USER");
        Product product = createProduct("Last Unit Med", 1599, 1, null);

        // Two independent checkout threads racing on stock == 1
        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();
        SessionCart cart1 = cartService.getCart(session1);
        SessionCart cart2 = cartService.getCart(session2);
        cart1.addProduct(product.getReference());
        cart2.addProduct(product.getReference());

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> task1 = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    return orderService.setOrder(buyer1, cart1, SaleStatus.PENDING, paymentDetails(buyer1), session1);
                } catch (Exception ex) {
                    return ex;
                }
            };
            Callable<Object> task2 = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    return orderService.setOrder(buyer2, cart2, SaleStatus.PENDING, paymentDetails(buyer2), session2);
                } catch (Exception ex) {
                    return ex;
                }
            };

            List<Future<Object>> results = pool.invokeAll(List.of(task1, task2));
            List<Order> succeeded = new ArrayList<>();
            List<ResponseStatusException> conflicts = new ArrayList<>();

            for (Future<Object> future : results) {
                Object outcome = future.get(10, TimeUnit.SECONDS);
                if (outcome instanceof Order o) {
                    succeeded.add(o);
                } else if (outcome instanceof ResponseStatusException rse) {
                    conflicts.add(rse);
                } else {
                    throw new AssertionError("Unexpected outcome: " + outcome);
                }
            }

            assertThat(succeeded).hasSize(1);
            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            Product refreshed = productRepository.findByReference(product.getReference()).orElseThrow();
            assertThat(refreshed.getStock()).isEqualTo(0);
            assertThat(stockReservationService.getAvailableStock(refreshed, null)).isEqualTo(0);
        } finally {
            pool.shutdownNow();
            cartService.clearCart(session1);
            cartService.clearCart(session2);
        }
    }

    @Test
    void atomicDbDecrementPreventsOversellingEvenUnderDirectConcurrentUpdates() throws Exception {
        Product product = createProduct("Atomic Stock Med", 1000, 1, null);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> decrementTask = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                return executeAtomicDecrementInTx(product.getReference(), 1);
            };
            List<Future<Integer>> futures = pool.invokeAll(List.of(decrementTask, decrementTask));
            int totalUpdated = futures.get(0).get(5, TimeUnit.SECONDS) + futures.get(1).get(5, TimeUnit.SECONDS);
            assertThat(totalUpdated).isEqualTo(1);
            assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(0);
        } finally {
            pool.shutdownNow();
        }
    }

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private int executeAtomicDecrementInTx(String ref, int qty) {
        Integer updated = transactionTemplate.execute(status -> productRepository.decrementStock(ref, qty));
        return updated != null ? updated : 0;
    }

    @Test
    void priceAndNameEditAfterOrderDoesNotAlterRenderedOrderLineSnapshots() throws Exception {
        User buyer = createUser("USER");
        User admin = createUser("ADMIN");
        Product product = createProduct("Original Vitamin Name", 1250, 10, admin);

        MockHttpSession session = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();

        Order order = orderService.setOrder(buyer, cartService.getCart(session), SaleStatus.PENDING, paymentDetails(buyer), session);
        cartService.clearCart(session);

        // Admin edits product price and name AFTER order was placed
        ProductDTO editDto = new ProductDTO();
        editDto.setName("Rebranded Expensive Vitamin");
        editDto.setDescription("Updated description");
        editDto.setPrice(new BigDecimal("99.95"));
        editDto.setStock(8);
        editDto.setReference(product.getReference());
        editDto.setCategory(Category.MEDICINE);
        productsService.updateProduct(editDto, product.getReference(), null);

        // Service DTO must retain snapshotted unitPrice (12.50), subtotal (25.00), and original productName
        OrderDetailsDTO details = orderService.getOrder(buyer.getId(), order.getId());
        assertThat(details.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductReference()).isEqualTo(product.getReference());
            assertThat(item.getProductName()).isEqualTo("Original Vitamin Name");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("12.50");
            assertThat(item.getAmount()).isEqualTo(2);
            assertThat(item.getSubtotal()).isEqualByComparingTo("25.00");
        });

        // Rendered HTML view must also show the snapshotted price & name, not 99,95 €
        mockMvc.perform(get("/user/dashboard/orders/" + order.getId()).with(user(buyer)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Original Vitamin Name")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12,50 €")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("25,00 €")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("99,95 €"))));
    }

    @Test
    void legacySeededOrderWithoutSnapshotsFallsBackToLiveProductValues() {
        // Order #1 from data.sql has null unitPriceCents & productName
        User admin = userRepository.findByUsername("juan").orElseThrow();
        OrderDetailsDTO details = orderService.getOrder(admin.getId(), 1L);
        assertThat(details.getItems()).hasSize(2);
        assertThat(details.getItems()).allSatisfy(item -> {
            assertThat(item.getProductName()).isNotBlank();
            assertThat(item.getUnitPrice()).isPositive();
            assertThat(item.getSubtotal()).isPositive();
        });
    }

    @Test
    void statusStateMachineEnforcesValidTransitionsAndRestoresStockOnCancelAndDelete() {
        User buyer = createUser("USER");
        Product product = createProduct("Restorable Med", 1000, 10, null);

        MockHttpSession session = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();

        Order order = orderService.setOrder(buyer, cartService.getCart(session), SaleStatus.PENDING, paymentDetails(buyer), session);
        cartService.clearCart(session);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(7);

        // Transition PENDING -> CANCELLED (with whitespace/lowercase) restores stock
        OrderDTO cancelled = orderService.updateOrder(order.getId(), "  cancelled  ");
        assertThat(cancelled.getStatus()).isEqualTo(SaleStatus.CANCELLED);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(10);

        // Re-cancelling an already-cancelled order must NOT double-restore stock
        orderService.updateOrder(order.getId(), "CANCELLED");
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(10);

        // Invalid transition CANCELLED -> COMPLETED must fail with 400
        assertThatThrownBy(() -> orderService.updateOrder(order.getId(), "COMPLETED"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        // Deleting an already-cancelled order must NOT double-restore stock
        orderService.deleteOrderById(order.getId());
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(10);

        // Create a second order, transition PENDING -> SHIPPED -> COMPLETED, verify COMPLETED -> CANCELLED rejected
        MockHttpSession session2 = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session2)).isTrue();
        Order order2 = orderService.setOrder(buyer, cartService.getCart(session2), SaleStatus.PENDING, paymentDetails(buyer), session2);
        cartService.clearCart(session2);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(9);

        orderService.updateOrder(order2.getId(), "SHIPPED");
        assertThatThrownBy(() -> orderService.updateOrder(order2.getId(), "PENDING"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        orderService.updateOrder(order2.getId(), "COMPLETED");
        assertThatThrownBy(() -> orderService.updateOrder(order2.getId(), "CANCELLED"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        // Create a third PENDING order and delete it directly -> restores stock
        MockHttpSession session3 = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session3)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session3)).isTrue();
        Order order3 = orderService.setOrder(buyer, cartService.getCart(session3), SaleStatus.PENDING, paymentDetails(buyer), session3);
        cartService.clearCart(session3);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(7);

        orderService.deleteOrderById(order3.getId());
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(9);
    }

    @Test
    void nonOwnerQueryingAnotherUsersOrderReceivesUnified404WhileAdminCanAccess() throws Exception {
        User owner = createUser("USER");
        User otherUser = createUser("USER");
        User admin = createUser("ADMIN");
        Product product = createProduct("Private Order Item", 900, 5, null);

        MockHttpSession session = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        Order order = orderService.setOrder(owner, cartService.getCart(session), SaleStatus.PENDING, paymentDetails(owner), session);
        cartService.clearCart(session);

        // Non-owner gets 404 (NOT 403), indistinguishable from non-existent order
        assertThatThrownBy(() -> orderService.getOrder(otherUser.getId(), order.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/user/dashboard/orders/" + order.getId()).with(user(otherUser)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/user/dashboard/orders/9999999").with(user(otherUser)))
                .andExpect(status().isNotFound());

        // Owner and admin can access
        mockMvc.perform(get("/user/dashboard/orders/" + order.getId()).with(user(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard/orders/" + order.getId()).with(user(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void orderDtoPreservesBigDecimalPrecisionAndConvertsUtcToEuropeMadrid() {
        Order order = new Order();
        order.setId(42L);
        order.setTotalPrice(new BigDecimal("123456.78"));
        order.setStatus(SaleStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.PAYPAL);
        // 23:30 UTC on July 15 is 01:30 CEST (+02:00) on July 16 in Europe/Madrid
        order.setCreatedAt(LocalDateTime.of(2025, 7, 15, 23, 30, 0));
        order.setUpdatedAt(LocalDateTime.of(2025, 1, 10, 23, 30, 0));

        OrderDTO dto = orderMapper.orderToOrderDTO(order);
        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getTotalPrice()).isEqualByComparingTo(new BigDecimal("123456.78"));
        assertThat(dto.getCreatedAt()).isEqualTo("16-07-2025");
        assertThat(dto.getUpdatedAt()).isEqualTo("11-01-2025");
    }

    @Test
    void productUpdatePreservesUserModifiedAndOptimisticLockBlocksStaleStockOverwrite() {
        User admin = createUser("ADMIN");
        Product product = createProduct("Lock Test Product", 2000, 10, admin);

        // Update product without SecurityContext -> userModified must NOT be wiped to null
        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("Lock Test Product v2");
        updateDto.setDescription("Updated desc");
        updateDto.setPrice(new BigDecimal("21.50"));
        updateDto.setStock(10);
        updateDto.setReference(product.getReference());
        updateDto.setCategory(Category.MEDICINE);
        productsService.updateProduct(updateDto, product.getReference(), null);

        Product reloaded = productRepository.findByReference(product.getReference()).orElseThrow();
        assertThat(reloaded.getUserModified()).isNotNull();
        assertThat(reloaded.getUserModified().getId()).isEqualTo(admin.getId());
        assertThat(reloaded.getName()).isEqualTo("Lock Test Product v2");
        assertThat(reloaded.getPrice()).isEqualTo(2150);

        // Stale entity loaded before atomic stock decrement must fail with OptimisticLockingFailureException
        Product staleCopy = productRepository.findByReference(product.getReference()).orElseThrow();
        assertThat(executeAtomicDecrementInTx(product.getReference(), 2)).isEqualTo(1);

        staleCopy.setStock(10);
        assertThatThrownBy(() -> productRepository.saveAndFlush(staleCopy))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(8);
    }

    @Test
    void concurrentCancellationsRestoreStockExactlyOnce() throws Exception {
        User buyer = createUser("USER");
        Product product = createProduct("Concurrent Cancel Med", 1200, 10, null);

        MockHttpSession session = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();

        Order order = orderService.setOrder(buyer, cartService.getCart(session), SaleStatus.PENDING, paymentDetails(buyer), session);
        cartService.clearCart(session);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(7);

        int threads = 4;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<OrderDTO>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    return orderService.updateOrder(order.getId(), "CANCELLED");
                });
            }
            List<Future<OrderDTO>> futures = pool.invokeAll(tasks);
            for (Future<OrderDTO> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(SaleStatus.CANCELLED);
            }
            assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentCancelAndDeleteRestoreStockExactlyOnce() throws Exception {
        User buyer = createUser("USER");
        Product product = createProduct("Cancel Delete Race Med", 1200, 10, null);

        MockHttpSession session = new MockHttpSession();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();
        assertThat(cartService.addProductToCart(product.getReference(), session)).isTrue();

        Order order = orderService.setOrder(buyer, cartService.getCart(session), SaleStatus.PENDING, paymentDetails(buyer), session);
        cartService.clearCart(session);
        assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(8);

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> cancelTask = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    return orderService.updateOrder(order.getId(), "CANCELLED");
                } catch (ResponseStatusException ex) {
                    return ex;
                }
            };
            Callable<Object> deleteTask = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    orderService.deleteOrderById(order.getId());
                    return "DELETED";
                } catch (ResponseStatusException ex) {
                    return ex;
                }
            };
            List<Future<Object>> futures = pool.invokeAll(List.of(cancelTask, deleteTask));
            for (Future<Object> future : futures) {
                Object result = future.get(10, TimeUnit.SECONDS);
                if (result instanceof ResponseStatusException rse) {
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                }
            }
            assertThat(productRepository.findByReference(product.getReference()).orElseThrow().getStock()).isEqualTo(10);
            assertThat(orderRepository.findById(order.getId())).isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rollbackDuringOrderCreationPreservesActiveStockReservation() {
        User buyer = createUser("USER");
        // Ensure lexicographical order p1 < p2 so p1 decrements first, then p2 fails & triggers rollback
        Product p1 = createProduct("Rollback First", 1000, 3, null);
        Product p2 = createProduct("Rollback Second", 1000, 2, null);
        String firstRef = p1.getReference().compareTo(p2.getReference()) < 0 ? p1.getReference() : p2.getReference();
        String secondRef = p1.getReference().compareTo(p2.getReference()) < 0 ? p2.getReference() : p1.getReference();
        Product firstProd = productRepository.findByReference(firstRef).orElseThrow();
        Product secondProd = productRepository.findByReference(secondRef).orElseThrow();

        MockHttpSession session = new MockHttpSession();
        try {
            assertThat(cartService.addProductToCart(firstRef, session)).isTrue();
            assertThat(cartService.addProductToCart(secondRef, session)).isTrue();
            String reservationId = stockReservationService.getReservationId(session);
            assertThat(reservationId).isNotBlank();

            // Drain second product directly in DB so atomic decrement fails after first product decrements
            assertThat(executeAtomicDecrementInTx(secondRef, secondProd.getStock())).isEqualTo(1);

            assertThatThrownBy(() -> orderService.setOrder(
                    buyer, cartService.getCart(session), SaleStatus.PENDING, paymentDetails(buyer), session))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

            // First product stock rolled back, and in-memory reservation was NOT released on rollback
            assertThat(productRepository.findByReference(firstRef).orElseThrow().getStock())
                    .isEqualTo(firstProd.getStock());
            assertThat(stockReservationService.isReservationValid(session, cartService.getCart(session))).isTrue();
        } finally {
            cartService.clearCart(session);
        }
    }
}
