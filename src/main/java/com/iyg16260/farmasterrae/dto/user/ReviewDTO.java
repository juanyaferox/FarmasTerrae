package com.iyg16260.farmasterrae.dto.user;

import lombok.Data;

@Data
public class ReviewDTO {
    long id;
    String title;
    String content;
    int scoring;
    String productReference;
    String productImage;
    String productName;
}
