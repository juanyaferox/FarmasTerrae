package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper (componentModel = "spring") // Para que Spring lo gestione como un Bean
public interface ProductMapper {

    @Mapping(target = "price", source = "price", qualifiedByName = "formatPrice")
    ProductDTO productToProductDTO(Product product); // Asume que tienes una entidad Product

    @Mapping(target = "price", source = "price", qualifiedByName = "formatPrice")
    ProductPageDTO productToProductPageDTO(Product product);

    Product productDTOToProduct(ProductDTO productDTO);

    @Named("formatPrice")
    default BigDecimal formatPrice(Integer price) {
        return BigDecimal.valueOf(price).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

    }
}
