package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Local Docker demo path (prodminio): the {@code dev} context applies the demo dataset, and the
 * admin bootstrap is skipped because the demo already contains ADMIN accounts.
 */
@SpringBootTest (properties = {
        "spring.datasource.url=jdbc:h2:mem:liquibasedevseed;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE",
        "spring.sql.init.mode=never",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:/db/changelog/data-only-changelog.yaml",
        "spring.liquibase.contexts=dev",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
        "app.admin-bootstrap.username=bootstrap_admin",
        "app.admin-bootstrap.email=bootstrap.admin@example.com",
        "app.admin-bootstrap.password=Bootstrap123"
})
class LiquibaseDevSeedTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void seedsTheDemoDataset() {
        assertThat(userRepository.findByUsername("admin_juan"))
                .hasValueSatisfying(user -> assertThat(user.getProfile().getType()).isEqualTo("ADMIN"));
        assertThat(userRepository.count()).isEqualTo(15);
        assertThat(productRepository.count()).isPositive();
        assertThat(orderRepository.count()).isPositive();
        assertThat(reviewRepository.count()).isPositive();
    }

    @Test
    void skipsTheAdminBootstrapWhenAnAdminAlreadyExists() {
        assertThat(userRepository.findByUsername("bootstrap_admin")).isEmpty();
    }
}
