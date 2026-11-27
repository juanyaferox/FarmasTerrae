package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.CreateReviewDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.dto.user.UpdateReviewDTO;
import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.service.S3StorageService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ReviewMapper {
    @Autowired protected S3StorageService s3StorageService;

    @Mapping(target = "productReference", source = "product.reference")
    @Mapping(target = "productImage", source = "product.imagePath")
    @Mapping(target = "productName", source = "product.name")
    public abstract ReviewDTO reviewToReviewDTO(Review review);

    @AfterMapping
    protected void signProductImage(@MappingTarget ReviewDTO dto) {
        dto.setProductImage(s3StorageService.resolveImageUrl(dto.getProductImage()));
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "user", ignore = true)
    public abstract Review createReviewDTOToReview(CreateReviewDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "user", ignore = true)
    public abstract void updateReviewFromUpdateDTO(UpdateReviewDTO dto, @MappingTarget Review review);
}
