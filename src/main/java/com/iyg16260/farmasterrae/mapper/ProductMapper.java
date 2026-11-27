package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.service.S3StorageService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper (componentModel = "spring")
public abstract class ProductMapper {

    @Autowired
    protected S3StorageService s3StorageService;


    @Mapping (target = "price", source = "price", qualifiedByName = "formatPrice")
    public abstract ProductDTO productToProductDTO(Product product);

    @Mapping (target = "price", source = "price", qualifiedByName = "formatPrice")
    public abstract ProductPageDTO productToProductPageDTO(Product product);

    @Mapping (target = "price", source = "price", qualifiedByName = "formatPriceToCents")
    @Mapping (target = "id", ignore = true)
    @Mapping (target = "version", ignore = true)
    @Mapping (target = "userModified", ignore = true)
    @Mapping (target = "orderDetails", ignore = true)
    @Mapping (target = "reviewList", ignore = true)
    @Mapping (target = "imagePath", ignore = true)
    @Mapping (target = "deletedAt", ignore = true)
    public abstract Product productDTOToProduct(ProductDTO productDTO);

    @AfterMapping
    protected void addSignedUrl(Product product, @MappingTarget ProductDTO dto) {
        dto.setImagePath(s3StorageService.resolveImageUrl(dto.getImagePath()));
    }

    @AfterMapping
    protected void addSignedUrl(Product product, @MappingTarget ProductPageDTO dto) {
        dto.setImagePath(s3StorageService.resolveImageUrl(dto.getImagePath()));
    }

    @Named ("formatPrice")
    protected BigDecimal formatPrice(Integer price) {
        return price == null ? null : BigDecimal.valueOf(price).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Named ("formatPriceToCents")
    protected Integer formatPriceToCents(BigDecimal price) {
        return price == null ? null : price.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValueExact();
    }
}
