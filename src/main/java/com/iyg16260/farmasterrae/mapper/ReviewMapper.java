package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper (componentModel = "spring")
public interface ReviewMapper {
    @Mapping (target = "productReference", source = "product.reference")
    @Mapping (target = "productImage", source = "product.imagePath")
    @Mapping (target = "productName", source = "product.name")
    ReviewDTO reviewToReviewDTO(Review review);

    @Mapping (target = "product", ignore = true)
    @Mapping (target = "user", ignore = true)
    Review reviewDTOToReview(ReviewDTO reviewDTO);

    @Mapping (target = "product", ignore = true)
    @Mapping (target = "user", ignore = true)
    void updateReviewFromReviewDTO(ReviewDTO reviewDTO, @MappingTarget Review review);
}
