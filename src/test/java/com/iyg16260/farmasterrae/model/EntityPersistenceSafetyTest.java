package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.service.UserService;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Entities loaded by Hibernate: equality works across persistence contexts and against lazy proxies, and
 * toString never touches a lazy association (so it is safe on detached entities).
 */
@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:entitypersistencesafetydb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
class EntityPersistenceSafetyTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void theSameAccountLoadedTwiceIsOnePrincipalOfTheSessionRegistry() {
        User saved = createUser();
        User firstLoad = (User) userService.loadUserByUsername(saved.getUsername());
        User secondLoad = (User) userService.loadUserByUsername(saved.getEmail());

        assertThat(firstLoad).isNotSameAs(secondLoad).isEqualTo(secondLoad).hasSameHashCodeAs(secondLoad);
        assertThat(firstLoad).isNotEqualTo(userRepository.findByUsername("juan").orElseThrow());

        SessionRegistryImpl registry = new SessionRegistryImpl();
        registry.registerNewSession("browser", firstLoad);
        registry.registerNewSession("phone", secondLoad);
        assertThat(registry.getAllPrincipals()).hasSize(1);
        assertThat(registry.getAllSessions(secondLoad, false)).hasSize(2);
    }

    @Test
    void detachedEntitiesEqualLazyProxiesOfTheSameRow() {
        User detachedUser = createUser();
        Product detachedProduct = productRepository.findByReference("REFVIT001").orElseThrow();

        transactionTemplate.executeWithoutResult(tx -> {
            User userProxy = userRepository.getReferenceById(detachedUser.getId());
            Product productProxy = productRepository.getReferenceById(detachedProduct.getId());
            assertThat(Hibernate.isInitialized(userProxy)).isFalse();
            assertThat(Hibernate.isInitialized(productProxy)).isFalse();

            assertThat(detachedUser.equals(userProxy)).isTrue();
            assertThat(detachedProduct.equals(productProxy)).isTrue();
            assertThat(userProxy.equals(detachedUser)).isTrue();
            assertThat(productProxy).hasSameHashCodeAs(detachedProduct);
        });
    }

    @Test
    void toStringOfDetachedEntitiesNeverLoadsLazyAssociations() {
        User buyer = createUser();
        Long orderId = createOrderWithReview(buyer);

        // Loaded and detached: user, order lines, product and reviews are all uninitialized lazy references
        Order order = orderRepository.findById(orderId).orElseThrow();
        Review review = reviewRepository.findAll().stream()
                .filter(r -> r.getTitle().equals("Detached " + orderId)).findFirst().orElseThrow();
        Product product = productRepository.findByReference("REFVIT002").orElseThrow();
        User user = userRepository.findById(buyer.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(order.getUser())).isFalse();
        assertThat(Hibernate.isInitialized(order.getOrderDetails())).isFalse();
        assertThat(Hibernate.isInitialized(review.getProduct())).isFalse();

        assertThatCode(() -> {
            for (Object entity : List.of(order, review, product, user, user.getProfile())) {
                entity.toString();
                entity.hashCode();
            }
        }).doesNotThrowAnyException();
        assertThat(user.toString()).doesNotContain(user.getPassword()).doesNotContain("password");
    }

    private User createUser() {
        String username = "ent" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Entidad " + username);
        user.setAddress("Calle Entidad 1");
        user.setPassword(passwordEncoder.encode("StrongPassword9!"));
        user.setProfile(profileRepository.findByType("USER"));
        return userRepository.saveAndFlush(user);
    }

    private Long createOrderWithReview(User buyer) {
        return transactionTemplate.execute(tx -> {
            Product product = productRepository.findByReference("REFVIT002").orElseThrow();
            Order order = new Order();
            order.setUser(userRepository.getReferenceById(buyer.getId()));
            order.setStatus(SaleStatus.COMPLETED);
            order.setPaymentMethod(PaymentMethod.PAYPAL);
            order.setTotalPrice(new BigDecimal("13.47"));
            order.setName(buyer.getFullName());
            order.setAddress(buyer.getAddress());
            Order saved = orderRepository.save(order);
            OrderDetails line = new OrderDetails(saved, product);
            line.setAmount(1);
            line.setUnitPriceCents(product.getPrice());
            line.setProductName(product.getName());
            saved.setOrderDetails(List.of(line));

            Review review = new Review();
            review.setUser(userRepository.getReferenceById(buyer.getId()));
            review.setProduct(product);
            review.setTitle("Detached " + saved.getId());
            review.setContent("Contenido");
            review.setScoring(7);
            reviewRepository.save(review);
            orderRepository.flush();
            return saved.getId();
        });
    }
}
