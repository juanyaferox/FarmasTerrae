package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.FarmasterraeApplication;
import com.iyg16260.farmasterrae.config.StockReservationSessionListener;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest (properties = {
        "spring.datasource.url=jdbc:h2:mem:issue10reservationtestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE",
        "stock.reservation.timeout.minutes=15",
        "stock.reservation.max-per-product=20"
})
class StockReservationServiceTest {

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationSessionListener sessionListener;

    private final List<String> activeReservationIds = new ArrayList<>();

    @BeforeEach
    void resetClock() {
        stockReservationService.setClock(Clock.systemUTC());
    }

    @AfterEach
    void cleanupReservations() {
        stockReservationService.setClock(Clock.systemUTC());
        for (String id : activeReservationIds) {
            stockReservationService.releaseReservation(id);
        }
        activeReservationIds.clear();
    }

    private Product createProduct(int stock) {
        Product p = new Product();
        p.setName("Test Product " + UUID.randomUUID().toString().substring(0, 6));
        p.setDescription("Desc");
        p.setPrice(1000);
        p.setStock(stock);
        p.setReference("RES-" + UUID.randomUUID().toString().substring(0, 8));
        p.setCategory(Category.VITAMIN);
        return productRepository.saveAndFlush(p);
    }

    @Test
    void applicationEnablesScheduling() {
        assertThat(FarmasterraeApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
    }

    @Test
    void expiredReservationsAreIgnoredByGetAvailableStockAndRemovedByCleanup() {
        Instant baseTime = Instant.parse("2026-06-01T10:00:00Z");
        stockReservationService.setClock(Clock.fixed(baseTime, ZoneOffset.UTC));

        Product product = createProduct(5);
        MockHttpSession session = new MockHttpSession();
        SessionCart cart = new SessionCart();
        cart.addProduct(product.getReference());
        cart.addProduct(product.getReference());

        assertThat(stockReservationService.reserveStockForCart(session, cart)).isTrue();
        String reservationId = stockReservationService.getReservationId(session);
        activeReservationIds.add(reservationId);

        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(3);

        // Advance past timeout WITHOUT calling cleanupExpiredReservations() yet
        stockReservationService.setClock(Clock.fixed(baseTime.plusSeconds(16 * 60L), ZoneOffset.UTC));

        // getAvailableStock must ignore expired reservations immediately
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(5);
        assertThat(stockReservationService.isReservationExpired(session)).isTrue();

        // Scheduled cleanup removes expired entries from memory
        stockReservationService.cleanupExpiredReservations();
        assertThat(stockReservationService.getReservationSnapshot(reservationId)).isNull();
    }

    @Test
    void sessionDestroyedListenerReleasesReservation() {
        Product product = createProduct(4);
        MockHttpSession session = new MockHttpSession();
        SessionCart cart = new SessionCart();
        cart.addProduct(product.getReference());
        cart.addProduct(product.getReference());

        assertThat(stockReservationService.reserveStockForCart(session, cart)).isTrue();
        String reservationId = stockReservationService.getReservationId(session);
        activeReservationIds.add(reservationId);
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(2);

        sessionListener.sessionDestroyed(new HttpSessionEvent(session));

        assertThat(stockReservationService.getReservationSnapshot(reservationId)).isNull();
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(4);
    }

    @Test
    void failedReservationPreservesExistingSessionReservationIntact() {
        Product p1 = createProduct(2);
        Product p2 = createProduct(0);

        MockHttpSession session = new MockHttpSession();
        SessionCart cart = new SessionCart();
        cart.addProduct(p1.getReference());

        assertThat(stockReservationService.reserveStockForCart(session, cart)).isTrue();
        String reservationId = stockReservationService.getReservationId(session);
        activeReservationIds.add(reservationId);
        assertThat(stockReservationService.getAvailableStock(p1, null)).isEqualTo(1);

        // Try to reserve an additional out-of-stock product
        cart.addProduct(p2.getReference());
        assertThat(stockReservationService.reserveStockForCart(session, cart)).isFalse();

        // Existing reservation for p1 must remain intact!
        assertThat(stockReservationService.getReservationSnapshot(reservationId))
                .containsExactlyEntriesOf(Map.of(p1.getReference(), 1));
        assertThat(stockReservationService.getAvailableStock(p1, null)).isEqualTo(1);
    }

    @Test
    void perSessionReservationCapIsEnforcedWithoutDroppingPriorReservation() {
        Product product = createProduct(50);
        MockHttpSession session = new MockHttpSession();
        SessionCart cart = new SessionCart();
        for (int i = 0; i < 20; i++) {
            cart.addProduct(product.getReference());
        }

        assertThat(stockReservationService.reserveStockForCart(session, cart)).isTrue();
        String reservationId = stockReservationService.getReservationId(session);
        activeReservationIds.add(reservationId);
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(30);

        // 21st unit exceeds cap of 20
        cart.addProduct(product.getReference());
        assertThat(stockReservationService.reserveStockForCart(session, cart)).isFalse();

        // Previous reservation of 20 stays intact
        assertThat(stockReservationService.getReservationSnapshot(reservationId))
                .containsExactlyEntriesOf(Map.of(product.getReference(), 20));
        assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(30);
    }

    @Test
    void validateCartReservationsVerifiesExpiryInMemoryPresenceAndExactQuantities() {
        Product product = createProduct(1);
        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();

        assertThat(cartService.addProductToCart(product.getReference(), session1)).isTrue();
        String resId1 = stockReservationService.getReservationId(session1);
        activeReservationIds.add(resId1);

        SessionCart cart1 = cartService.getCart(session1);
        assertThat(stockReservationService.isReservationValid(session1, cart1)).isTrue();
        assertThat(cartService.validateCartReservations(session1)).isTrue();

        // Case 1: Cart quantity drifts above in-memory reservation while no more stock exists
        cart1.addProduct(product.getReference()); // cart now has 2, reservation has 1, stock is 1
        assertThat(stockReservationService.isReservationValid(session1, cart1)).isFalse();
        assertThat(cartService.validateCartReservations(session1)).isFalse();
        // Revert drift
        cart1.deleteOneProduct(product.getReference());
        assertThat(stockReservationService.isReservationValid(session1, cart1)).isTrue();

        // Case 2: In-memory reservation removed while session attribute reservationTime is still recent,
        // and another session claims the last unit
        stockReservationService.releaseReservation(resId1);
        assertThat(stockReservationService.isReservationValid(session1, cart1)).isFalse();

        assertThat(cartService.addProductToCart(product.getReference(), session2)).isTrue();
        activeReservationIds.add(stockReservationService.getReservationId(session2));

        // session1 still has non-expired reservationTime attribute, yet validateCartReservations must return false!
        assertThat(cartService.validateCartReservations(session1)).isFalse();
    }

    @Test
    void concurrentReservationsCannotOverReserveLastAvailableUnit() throws Exception {
        Product product = createProduct(1);
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<MockHttpSession> sessions = new ArrayList<>();
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                MockHttpSession session = new MockHttpSession();
                sessions.add(session);
                tasks.add(() -> {
                    SessionCart cart = new SessionCart();
                    cart.addProduct(product.getReference());
                    barrier.await(5, TimeUnit.SECONDS);
                    return stockReservationService.reserveStockForCart(session, cart);
                });
            }

            List<Future<Boolean>> futures = pool.invokeAll(tasks);
            long successes = 0;
            for (Future<Boolean> future : futures) {
                if (Boolean.TRUE.equals(future.get(5, TimeUnit.SECONDS))) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(stockReservationService.getAvailableStock(product, null)).isEqualTo(0);
        } finally {
            pool.shutdownNow();
            for (MockHttpSession s : sessions) {
                String id = stockReservationService.getReservationId(s);
                if (id != null) {
                    activeReservationIds.add(id);
                }
            }
        }
    }
}
