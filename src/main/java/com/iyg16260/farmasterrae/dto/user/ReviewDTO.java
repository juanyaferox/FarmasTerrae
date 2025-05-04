package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewDTO {
    Long id;
    @Size (max = 50)
    String title;
    @Size (max = 1000)
    String content;
    @Max (10)
    @Min (1)
    int scoring;
    String productReference;
    String productImage;
    String productName;
}
