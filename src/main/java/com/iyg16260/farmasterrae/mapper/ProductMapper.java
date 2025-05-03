package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.ProductForReviewDTO;
import com.iyg16260.farmasterrae.model.Product;
import org.mapstruct.Mapper;

@Mapper (componentModel = "spring") // Para que Spring lo gestione como un Bean
public interface ProductMapper {
    ProductDTO productToProductDTO(Product product); // Asume que tienes una entidad Product

    ProductForReviewDTO productToProductForReviewDTO(Product product);

    Product productDTOToProduct(ProductDTO productDTO);
}
