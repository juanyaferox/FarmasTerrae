package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.model.Review;
import org.mapstruct.Mapper;

@Mapper (componentModel = "spring")
public interface ReviewMapper {
    ReviewDTO reviewToReviewDTO(Review review); // Asume que tienes una entidad Product

    Review reviewDTOToReview(ReviewDTO reviewDTO);
}
