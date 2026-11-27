package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Lombok on the JPA entities: toString/equals/hashCode never walk associations (no recursion through the
 * bidirectional relations, no lazy loading), toString never prints the password hash, and equality is by id.
 */
class EntityEqualityAndToStringTest {

    private static final String PASSWORD_HASH = "$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.";

    @Test
    void cyclicEntityGraphCanBePrintedAndHashed() {
        Graph graph = new Graph();

        assertThatCode(() -> {
            for (Object entity : graph.all()) {
                entity.toString();
                entity.hashCode();
                entity.equals(entity);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void toStringPrintsOwnColumnsButNeitherAssociationsNorThePassword() {
        Graph graph = new Graph();

        assertThat(graph.user.toString())
                .contains("username=alice")
                .doesNotContain("password")
                .doesNotContain(PASSWORD_HASH)
                .doesNotContain("profile=")
                .doesNotContain("orderList=")
                .doesNotContain("reviewList=")
                .doesNotContain("productList=");
        assertThat(graph.order.toString()).contains("id=21").doesNotContain("user=").doesNotContain("orderDetails=");
        assertThat(graph.line.toString()).contains("amount=2").doesNotContain("order=").doesNotContain("product=");
        assertThat(graph.product.toString()).contains("reference=REF-11")
                .doesNotContain("userModified=").doesNotContain("orderDetails=").doesNotContain("reviewList=");
        assertThat(graph.review.toString()).contains("title=Muy bueno").doesNotContain("user=").doesNotContain("product=");
        assertThat(graph.profile.toString()).contains("type=USER").doesNotContain("userList=");
        for (Object entity : graph.all()) {
            assertThat(entity.toString()).doesNotContain(PASSWORD_HASH);
        }
    }

    @Test
    void unsavedEntitiesAreOnlyEqualToThemselves() {
        List<Object[]> pairs = List.of(
                new Object[]{new User(), new User()},
                new Object[]{new Product(), new Product()},
                new Object[]{new Order(), new Order()},
                new Object[]{new Review(), new Review()},
                new Object[]{new Profile(), new Profile()},
                new Object[]{new OrderDetails(), new OrderDetails()},
                new Object[]{new OrderDetails(new Order(), product(3L)), new OrderDetails(new Order(), product(3L))});

        for (Object[] pair : pairs) {
            assertThat(pair[0]).isEqualTo(pair[0]).isNotEqualTo(pair[1]);
            assertThat(pair[0].hashCode()).isEqualTo(pair[0].hashCode());
            assertThat(new HashSet<>(List.of(pair[0], pair[1]))).hasSize(2);
        }
    }

    @Test
    void savedEntitiesAreEqualByIdWhateverTheirOtherFields() {
        User user = user(5L, "alice");
        User sameAccountRenamed = user(5L, "alice_new");
        sameAccountRenamed.setPassword("other-hash");
        assertEqualById(user, sameAccountRenamed, user(6L, "alice"));

        Product product = product(1L);
        Product sameProductEdited = product(1L);
        sameProductEdited.setName("Otro nombre");
        sameProductEdited.setStock(0);
        assertEqualById(product, sameProductEdited, product(2L));

        assertEqualById(order(1L, user), order(1L, user(9L, "bob")), order(2L, user));
        assertEqualById(review(1L, user, product), review(1L, user, product(2L)), review(2L, user, product));
        assertEqualById(profile(1L, "ADMIN"), profile(1L, "admin"), profile(2L, "ADMIN"));
        assertEqualById(new OrderDetails(order(1L, user), product(1L)),
                new OrderDetails(order(1L, user(9L, "bob")), product(1L)),
                new OrderDetails(order(1L, user), product(2L)));
    }

    @Test
    void entitiesOfDifferentTypesAreNeverEqual() {
        assertThat((Object) product(1L)).isNotEqualTo(order(1L, null));
        assertThat((Object) user(1L, "alice")).isNotEqualTo(profile(1L, "USER"));
        assertThat(product(1L)).isNotEqualTo(null);
    }

    @Test
    void sessionRegistryTreatsEveryLoadOfTheSameAccountAsOnePrincipal() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        User atLogin = user(42L, "alice");
        // Another request loads the same account again: a different instance with the same id
        User loadedLater = user(42L, "alice");

        registry.registerNewSession("first-device", atLogin);
        registry.registerNewSession("second-device", loadedLater);

        assertThat(registry.getAllPrincipals()).hasSize(1);
        assertThat(registry.getAllSessions(loadedLater, false))
                .extracting(SessionInformation::getSessionId)
                .containsExactlyInAnyOrder("first-device", "second-device");

        // Renaming the account changes no key: its sessions can still be found and revoked
        atLogin.setUsername("alice_new");
        assertThat(registry.getAllSessions(user(42L, "whatever"), false)).hasSize(2);
        assertThat(registry.getAllSessions(user(43L, "alice"), false)).isEmpty();
    }

    @Test
    void sessionRegistryNeverMergesTwoUnsavedUsers() {
        SessionRegistryImpl registry = new SessionRegistryImpl();

        registry.registerNewSession("one", new User());
        registry.registerNewSession("two", new User());

        assertThat(registry.getAllPrincipals()).hasSize(2);
    }

    @Test
    void hashCodeStaysStableInHashCollectionsWhenGeneratedIdIsAssigned() {
        User u = new User();
        Product p = new Product();
        Order o = new Order();
        Review r = new Review();
        Profile pr = new Profile();
        OrderDetails od = new OrderDetails();

        Set<Object> set = new HashSet<>(List.of(u, p, o, r, pr, od));

        u.setId(101L);
        p.setId(102L);
        o.setId(103L);
        r.setId(104L);
        pr.setId(105L);
        od.setId(new OrderDetailsId(103L, 102L));

        assertThat(set).contains(u, p, o, r, pr, od);
        assertThat(set).contains(user(101L, "persisted"), product(102L), order(103L, u), review(104L, u, p),
                profile(105L, "USER"), new OrderDetails(order(103L, u), product(102L)));
    }

    private static void assertEqualById(Object entity, Object sameId, Object otherId) {
        assertThat(entity).isEqualTo(sameId).hasSameHashCodeAs(sameId).isNotEqualTo(otherId);
        assertThat(sameId).isEqualTo(entity);
        assertThat(Set.of(entity, otherId)).contains(sameId);
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(PASSWORD_HASH);
        user.setProfile(profile(2L, "USER"));
        return user;
    }

    private static Profile profile(Long id, String type) {
        Profile profile = new Profile();
        profile.setId(id);
        profile.setType(type);
        profile.setDescription(type.toLowerCase());
        return profile;
    }

    private static Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setReference("REF-" + id);
        product.setName("Producto " + id);
        product.setPrice(1250);
        product.setStock(10);
        product.setCategory(Category.CARE);
        return product;
    }

    private static Order order(Long id, User user) {
        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setStatus(SaleStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.PAYPAL);
        order.setTotalPrice(new BigDecimal("25.00"));
        return order;
    }

    private static Review review(Long id, User user, Product product) {
        Review review = new Review();
        review.setId(id);
        review.setUser(user);
        review.setProduct(product);
        review.setTitle("Muy bueno");
        review.setScoring(9);
        return review;
    }

    /** Every bidirectional association wired both ways, as Hibernate leaves them once loaded */
    private static final class Graph {
        final Profile profile = profile(2L, "USER");
        final User user = user(7L, "alice");
        final Product product = product(11L);
        final Order order = order(21L, user);
        final OrderDetails line = new OrderDetails(order, product);
        final Review review = review(31L, user, product);

        Graph() {
            user.setProfile(profile);
            profile.setUserList(new ArrayList<>(List.of(user)));
            line.setAmount(2);
            order.setOrderDetails(new ArrayList<>(List.of(line)));
            product.setOrderDetails(new ArrayList<>(List.of(line)));
            product.setReviewList(new ArrayList<>(List.of(review)));
            product.setUserModified(user);
            user.setOrderList(new ArrayList<>(List.of(order)));
            user.setReviewList(new ArrayList<>(List.of(review)));
            user.setProductList(new ArrayList<>(List.of(product)));
        }

        List<Object> all() {
            return List.of(profile, user, product, order, line, review);
        }
    }
}
