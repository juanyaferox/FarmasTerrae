package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production seeding path: Hibernate creates the schema, LiquibaseRunnerConfig applies the changelog
 * with the {@code prod} context, then AdminBootstrapRunner creates the configured admin.
 */
@SpringBootTest (properties = {
        "spring.datasource.url=jdbc:h2:mem:liquibaseprodseed;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE",
        "spring.sql.init.mode=never",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:/db/changelog/data-only-changelog.yaml",
        "spring.liquibase.contexts=prod",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
        "app.admin-bootstrap.username=Bootstrap_Admin",
        "app.admin-bootstrap.email=bootstrap.admin@example.com",
        "app.admin-bootstrap.password=Bootstrap123"
})
class LiquibaseProdSeedTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsOnlyTheProfilesAndNoDemoData() {
        assertThat(profileRepository.findAll()).extracting(Profile::getType).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(productRepository.count()).isZero();
        assertThat(orderRepository.count()).isZero();
        assertThat(reviewRepository.count()).isZero();
        assertThat(userRepository.findByUsername("admin_juan")).isEmpty();
        assertThat(userRepository.findByUsername("admin_maria")).isEmpty();
    }

    @Test
    void bootstrapsTheConfiguredAdminWithAnEncodedPassword() {
        assertThat(userRepository.findAll()).singleElement().satisfies(admin -> {
            assertThat(admin.getUsername()).isEqualTo("bootstrap_admin");
            assertThat(admin.getEmail()).isEqualTo("bootstrap.admin@example.com");
            assertThat(admin.getProfile().getType()).isEqualTo("ADMIN");
            assertThat(admin.getPassword()).isNotEqualTo("Bootstrap123");
            assertThat(passwordEncoder.matches("Bootstrap123", admin.getPassword())).isTrue();
        });
        assertThat(userRepository.findAllByProfileType("ADMIN")).extracting(User::getUsername)
                .containsExactly("bootstrap_admin");
    }
}
