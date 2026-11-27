package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest (properties = "spring.datasource.url=jdbc:h2:mem:productmappertestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @MockitoBean
    private S3StorageService s3StorageService;

    @Test
    void productToDtosPreservesAllTextFieldsAndOnlySignsImagePath() {
        when(s3StorageService.resolveImageUrl("product-images/vitc.jpg"))
                .thenReturn("https://signed.example/product-images/vitc.jpg");

        Product product = new Product();
        product.setName("Vitamina C 1000mg");
        product.setDescription("Comprimidos masticables");
        product.setReference("VIT-C-1000");
        product.setPrice(1295);
        product.setStock(42);
        product.setCategory(Category.VITAMIN);
        product.setImagePath("product-images/vitc.jpg");

        ProductDTO dto = productMapper.productToProductDTO(product);
        assertThat(dto.getName()).isEqualTo("Vitamina C 1000mg");
        assertThat(dto.getDescription()).isEqualTo("Comprimidos masticables");
        assertThat(dto.getReference()).isEqualTo("VIT-C-1000");
        assertThat(dto.getPrice()).isEqualByComparingTo("12.95");
        assertThat(dto.getStock()).isEqualTo(42);
        assertThat(dto.getCategory()).isEqualTo(Category.VITAMIN);
        assertThat(dto.getImagePath()).isEqualTo("https://signed.example/product-images/vitc.jpg");

        ProductPageDTO pageDto = productMapper.productToProductPageDTO(product);
        assertThat(pageDto.getName()).isEqualTo("Vitamina C 1000mg");
        assertThat(pageDto.getReference()).isEqualTo("VIT-C-1000");
        assertThat(pageDto.getPrice()).isEqualByComparingTo("12.95");
        assertThat(pageDto.getImagePath()).isEqualTo("https://signed.example/product-images/vitc.jpg");
    }

    @Test
    void productDtoToEntityPreservesTextFieldsIgnoresImagePathAndRoundsPriceHalfUp() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Ibuprofeno 600mg");
        dto.setDescription("Antiinflamatorio");
        dto.setReference("IBUP-600");
        dto.setPrice(new BigDecimal("12.345"));
        dto.setStock(15);
        dto.setCategory(Category.MEDICINE);
        dto.setImagePath("product-images/attacker-key.jpg");

        Product entity = productMapper.productDTOToProduct(dto);
        assertThat(entity.getName()).isEqualTo("Ibuprofeno 600mg");
        assertThat(entity.getDescription()).isEqualTo("Antiinflamatorio");
        assertThat(entity.getReference()).isEqualTo("IBUP-600");
        assertThat(entity.getPrice()).isEqualTo(1235);
        assertThat(entity.getStock()).isEqualTo(15);
        assertThat(entity.getCategory()).isEqualTo(Category.MEDICINE);
        assertThat(entity.getImagePath()).isNull();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void priceHelpersHandleNullAndRejectIntegerOverflow() {
        assertThat(productMapper.formatPrice(null)).isNull();
        assertThat(productMapper.formatPriceToCents(null)).isNull();
        assertThat(productMapper.formatPrice(105)).isEqualByComparingTo("1.05");
        assertThat(productMapper.formatPriceToCents(new BigDecimal("12.345"))).isEqualTo(1235);

        assertThatThrownBy(() -> productMapper.formatPriceToCents(new BigDecimal("99999999.99")))
                .isInstanceOf(ArithmeticException.class);
    }
}
