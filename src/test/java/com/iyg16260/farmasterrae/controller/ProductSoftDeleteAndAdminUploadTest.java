package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
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
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.StockReservationService;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:issue11producttestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class ProductSoftDeleteAndAdminUploadTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ProductsService productsService;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private StockReservationService stockReservationService;
    @Autowired private Validator validator;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private S3Client s3Client;
    @MockitoBean private S3Presigner s3Presigner;

    @BeforeEach
    void stubPresigner() throws Exception {
        reset(s3Client, s3Presigner);
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://signed.example/product-images/mock.jpg").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
    }

    private User createUser(String roleType) {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User u = new User();
        u.setUsername("p11_" + suffix);
        u.setEmail("p11_" + suffix + "@example.com");
        u.setFullName("Test User " + suffix);
        u.setAddress("Calle 11");
        u.setPassword(passwordEncoder.encode("StrongPassword9!"));
        u.setProfile(profileRepository.findByType(roleType));
        return userRepository.saveAndFlush(u);
    }

    private Product createProduct(String reference, int stock) {
        Product p = new Product();
        p.setName("Prod " + reference);
        p.setDescription("Desc " + reference);
        p.setPrice(1250);
        p.setStock(stock);
        p.setReference(reference);
        p.setCategory(Category.VITAMIN);
        return productRepository.saveAndFlush(p);
    }

    private byte[] validPngBytes() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        image.setRGB(1, 1, 0x22AA88);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void softDeletePreservesOrdersAndReviewsHidesProductAndRejectsDoubleDeleteOrRecreate() throws Exception {
        User admin = createUser("ADMIN");
        User buyer = createUser("USER");
        String ref = "ARC-" + UUID.randomUUID().toString().substring(0, 6);
        Product product = createProduct(ref, 10);

        // Attach a historical order line and a review to this product
        Order savedOrder = transactionTemplate.execute(status -> {
            Order order = new Order();
            order.setUser(userRepository.getReferenceById(buyer.getId()));
            order.setStatus(SaleStatus.COMPLETED);
            order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            order.setTotalPrice(new BigDecimal("12.50"));
            order.setName(buyer.getFullName());
            order.setAddress(buyer.getAddress());
            Order persisted = orderRepository.save(order);
            OrderDetails line = new OrderDetails(persisted, productRepository.getReferenceById(product.getId()));
            line.setAmount(1);
            line.setUnitPriceCents(1250);
            line.setProductName(product.getName());
            persisted.setOrderDetails(List.of(line));
            orderRepository.flush();
            return persisted;
        });

        Review review = new Review();
        review.setProduct(product);
        review.setUser(buyer);
        review.setTitle("Great");
        review.setContent("Worked nicely");
        review.setScoring(9);
        Review savedReview = reviewRepository.saveAndFlush(review);

        // Soft-delete via admin endpoint
        mockMvc.perform(delete("/admin/dashboard/products").with(user(admin)).with(csrf())
                        .param("reference", ref))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard/products"));

        // Historical order & review still readable without EntityNotFoundException
        assertThat(orderService.getOrder(buyer.getId(), savedOrder.getId()).getItems()).hasSize(1);
        assertThat(reviewRepository.findById(savedReview.getId())).isPresent();

        // Excluded from storefront details (404), catalog search, category filter, and admin list
        mockMvc.perform(get("/products/" + ref))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/products").param("search", ref))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(product.getName()))));
        mockMvc.perform(get("/products").param("category", "VITAMIN").param("search", ref))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(product.getName()))));
        mockMvc.perform(get("/admin/dashboard/products").with(user(admin)).param("search", ref))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(product.getName()))));
        assertThat(productsService.getRandomProducts(500))
                .extracting(ProductDTO::getReference)
                .doesNotContain(ref);

        // Deleting a second time returns 404
        mockMvc.perform(delete("/admin/dashboard/products").with(user(admin)).with(csrf())
                        .param("reference", ref))
                .andExpect(status().isNotFound());

        // Re-creating the same reference returns 400
        mockMvc.perform(multipart("/admin/dashboard/products").with(user(admin)).with(csrf())
                        .param("reference", ref)
                        .param("name", "Duplicate Ref")
                        .param("description", "Desc")
                        .param("price", "10.00")
                        .param("stock", "5")
                        .param("category", "VITAMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archivedProductIsPrunedFromCartWithoutPhantomReservationAndBlocksCheckout() throws Exception {
        User buyer = createUser("USER");
        Product toArchive = createProduct("PRUNE-E-" + UUID.randomUUID().toString().substring(0, 5), 5);
        Product active = createProduct("PRUNE-C-" + UUID.randomUUID().toString().substring(0, 5), 5);

        MockHttpSession session = new MockHttpSession();
        try {
            // Add E, then archive E
            assertThat(cartService.addProductToCart(toArchive.getReference(), session)).isTrue();
            productsService.deleteProduct(toArchive.getReference());

            // Adding active C while archived E is in cart must succeed, prune E, keep C reserved (no phantom reservation!)
            assertThat(cartService.addProductToCart(active.getReference(), session)).isTrue();
            SessionCart cart = cartService.getCart(session);
            assertThat(cart.getProducts()).containsOnlyKeys(active.getReference());
            assertThat(cart.getProducts().get(active.getReference())).isEqualTo(1);
            assertThat(stockReservationService.getAvailableStock(active, null)).isEqualTo(4);

            // Viewing /cart displays the notice and renders cleanly without 500
            mockMvc.perform(get("/cart").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Algunos productos ya no están disponibles")))
                    .andExpect(content().string(containsString(active.getName())));

            // Now proceed to /order, archive C before /order/confirm -> checkout redirects to /cart, 0 orders, stock unchanged
            mockMvc.perform(get("/order").session(session).with(user(buyer)))
                    .andExpect(status().isOk());
            String checkoutToken = (String) session.getAttribute(OrderController.CHECKOUT_TOKEN_SESSION_KEY);
            productsService.deleteProduct(active.getReference());
            long ordersBefore = orderRepository.count();

            mockMvc.perform(post("/order/confirm").session(session).with(user(buyer)).with(csrf())
                            .param("checkoutToken", checkoutToken)
                            .param("paymentMethod", PaymentMethod.CREDIT_CARD.name())
                            .param("fullName", buyer.getFullName())
                            .param("address", buyer.getAddress()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cart"));

            assertThat(orderRepository.count()).isEqualTo(ordersBefore);
            assertThat(productRepository.findByReference(active.getReference()).orElseThrow().getStock()).isEqualTo(5);
        } finally {
            cartService.clearCart(session);
        }
    }

    @Test
    void adminMultipartCreateUploadsImageIgnoresInboundImagePathAndRejectsInvalidUploads() throws Exception {
        User admin = createUser("ADMIN");
        String ref = "UPL-" + UUID.randomUUID().toString().substring(0, 6);
        MockMultipartFile validPng = new MockMultipartFile("imageFile", "pill.png", "image/png", validPngBytes());

        mockMvc.perform(multipart("/admin/dashboard/products")
                        .file(validPng)
                        .with(user(admin))
                        .with(csrf())
                        .param("reference", ref)
                        .param("name", "Uploaded Med")
                        .param("description", "Fresh description")
                        .param("price", "19.95")
                        .param("stock", "12")
                        .param("category", "MEDICINE")
                        .param("imagePath", "product-images/attacker-injected.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard/products"));

        Product created = productRepository.findByReference(ref).orElseThrow();
        assertThat(created.getName()).isEqualTo("Uploaded Med");
        assertThat(created.getReference()).isEqualTo(ref);
        assertThat(created.getImagePath()).startsWith("product-images/").endsWith(".jpg");
        assertThat(created.getImagePath()).isNotEqualTo("product-images/attacker-injected.jpg");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getValue().key()).isEqualTo(created.getImagePath());

        // Invalid disguised file must fail with 400 and never upload to S3
        reset(s3Client);
        MockMultipartFile fake = new MockMultipartFile("imageFile", "evil.jpg", "image/jpeg",
                "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/admin/dashboard/products")
                        .file(fake)
                        .with(user(admin))
                        .with(csrf())
                        .param("reference", "BAD-" + UUID.randomUUID().toString().substring(0, 5))
                        .param("name", "Bad Img Med")
                        .param("description", "Desc")
                        .param("price", "9.99")
                        .param("stock", "3")
                        .param("category", "MEDICINE"))
                .andExpect(status().isBadRequest());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void adminUpdateAllowsLegacyReferenceRejectsReferenceChangeAndCleansUpS3OnCommitAndRollback() throws Exception {
        User admin = createUser("ADMIN");
        Product legacy = createProduct("ESMAL-UÑAS-" + UUID.randomUUID().toString().substring(0, 4), 10);
        legacy.setImagePath("product-images/old-key.jpg");
        productRepository.saveAndFlush(legacy);

        // Editing a legacy non-ASCII reference WITHOUT changing it must succeed and delete old S3 key after commit
        MockMultipartFile newImg = new MockMultipartFile("imageFile", "new.png", "image/png", validPngBytes());
        mockMvc.perform(multipart("/admin/dashboard/products")
                        .file(newImg)
                        .with(user(admin))
                        .with(csrf())
                        .param("_method", "put")
                        .param("oldReference", legacy.getReference())
                        .param("reference", legacy.getReference())
                        .param("name", "Esmalte Renovado")
                        .param("description", "Secado rápido")
                        .param("price", "7.50")
                        .param("stock", "15")
                        .param("category", "BEAUTY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard/products"));

        Product updated = productRepository.findByReference(legacy.getReference()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Esmalte Renovado");
        assertThat(updated.getPrice()).isEqualTo(750);
        assertThat(updated.getImagePath()).startsWith("product-images/").isNotEqualTo("product-images/old-key.jpg");
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().key()).isEqualTo("product-images/old-key.jpg");

        // Attempting to change reference on PUT must fail with 400
        mockMvc.perform(multipart("/admin/dashboard/products")
                        .with(user(admin))
                        .with(csrf())
                        .param("_method", "put")
                        .param("oldReference", legacy.getReference())
                        .param("reference", "NEW-REF-123")
                        .param("name", "Esmalte Renovado")
                        .param("description", "Secado rápido")
                        .param("price", "7.50")
                        .param("stock", "15")
                        .param("category", "BEAUTY"))
                .andExpect(status().isBadRequest());

        // On DB rollback after upload, newly uploaded key is cleaned up, old key is NOT deleted,
        // and a secondary S3 delete failure does not mask the original exception!
        reset(s3Client);
        doThrow(S3Exception.builder().message("delete failed").statusCode(500).build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        ProductDTO overlongName = new ProductDTO();
        overlongName.setName("X".repeat(300)); // exceeds VARCHAR(255) -> saveAndFlush throws DataIntegrityViolationException
        overlongName.setDescription("Desc");
        overlongName.setPrice(new BigDecimal("10.00"));
        overlongName.setStock(5);
        overlongName.setCategory(Category.BEAUTY);
        MockMultipartFile rollbackImg = new MockMultipartFile("imageFile", "rb.png", "image/png", validPngBytes());

        assertThatThrownBy(() -> productsService.updateProduct(overlongName, legacy.getReference(), rollbackImg))
                .isInstanceOf(DataIntegrityViolationException.class);

        ArgumentCaptor<PutObjectRequest> uploadedCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(uploadedCaptor.capture(), any(RequestBody.class));
        ArgumentCaptor<DeleteObjectRequest> cleanupCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(cleanupCaptor.capture());
        assertThat(cleanupCaptor.getValue().key()).isEqualTo(uploadedCaptor.getValue().key());
        assertThat(cleanupCaptor.getValue().key()).isNotEqualTo(updated.getImagePath());
    }

    @Test
    void productDtoValidationEnforcesConstraintsAndAcceptsAllSeededReferences() throws Exception {
        ProductDTO invalid = new ProductDTO();
        invalid.setName("");
        invalid.setPrice(new BigDecimal("0.00"));
        invalid.setStock(-1);
        invalid.setReference("..");
        invalid.setCategory(null);

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(invalid, ProductDTO.OnCreate.class);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("name", "price", "stock", "reference", "category");
        assertThat(violations).allSatisfy(v -> assertThat(v.getMessage()).doesNotContain("must "));

        // Every seeded product reference in products.sql must pass OnCreate validation
        String sql = Files.readString(Path.of("src/main/resources/db/changelog/data/products.sql"));
        Matcher matcher = Pattern.compile(",\\s*'([^']+)',\\s*1,\\s*'(?:VITAMIN|MEDICINE|CARE|FIRST_AID|BEAUTY)'\\)").matcher(sql);
        int count = 0;
        while (matcher.find()) {
            count++;
            ProductDTO valid = new ProductDTO();
            valid.setName("Valid");
            valid.setDescription("Desc");
            valid.setPrice(new BigDecimal("9.99"));
            valid.setStock(10);
            valid.setReference(matcher.group(1));
            valid.setCategory(Category.VITAMIN);
            assertThat(validator.validate(valid, ProductDTO.OnCreate.class))
                    .as("Seeded reference %s should be valid", matcher.group(1))
                    .isEmpty();
        }
        assertThat(count).isEqualTo(100);
    }
}
