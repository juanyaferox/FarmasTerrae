package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.service.S3StorageService;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Mapper (componentModel = "spring")
public abstract class ProductMapper {

    @Autowired
    protected S3StorageService s3StorageService;

    private static final Duration DEFAULT_URL_DURATION = Duration.ofHours(2);

    @Mapping (target = "price", source = "price", qualifiedByName = "formatPrice")
    public abstract ProductDTO productToProductDTO(Product product);

    @Mapping (target = "price", source = "price", qualifiedByName = "formatPrice")
    public abstract ProductPageDTO productToProductPageDTO(Product product);

    @Mapping (target = "price", source = "price", qualifiedByName = "formatPriceToCents")
    public abstract Product productDTOToProduct(ProductDTO productDTO);

    @AfterMapping
    protected void addSignedUrl(Product product, @MappingTarget ProductDTO dto) {
        dto.setImagePath(generateSignedUrl(dto.getImagePath(), product.getReference()));
    }

    @AfterMapping
    protected void addSignedUrl(Product product, @MappingTarget ProductPageDTO dto) {
        dto.setImagePath(generateSignedUrl(dto.getImagePath(), product.getReference()));
    }

    private String generateSignedUrl(String imagePath, String reference) {
        if (StringUtils.isNotBlank(imagePath)) {
            try {
                String key = s3StorageService.extractKeyFromUrl(imagePath);
                if (key != null) {
                    return s3StorageService.generatePresignedUrl(key, DEFAULT_URL_DURATION);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generando URL firmada para producto " + reference + ": " + e.getMessage());
            }
        }
        return imagePath; // fallback
    }

    @Named ("formatPrice")
    protected BigDecimal formatPrice(Integer price) {
        return BigDecimal.valueOf(price).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Named ("formatPriceToCents")
    protected Integer formatPriceToCents(BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
