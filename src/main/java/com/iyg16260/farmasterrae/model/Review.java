package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table (name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_reviews_user_product", columnNames = {"userId", "productId"}))
public class Review {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Column (length = 50)
    String title;

    @Column (length = 1000)
    String content;

    Integer scoring;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "productId")
    Product product;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "userId")
    User user;
}
