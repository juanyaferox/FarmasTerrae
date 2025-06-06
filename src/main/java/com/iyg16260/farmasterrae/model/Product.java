package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.Category;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode (onlyExplicitlyIncluded = true)
@Table (name = "products", indexes = {@Index (name = "idxProductCategory", columnList = "category"),
        @Index (name = "idxProductReference", columnList = "reference")})
public class Product {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    String name;

    @Column (length = 1500)
    String description;

    String imagePath;

    Integer price;

    Integer stock;

    @Column (unique = true)
    String reference;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "userModifiedId")
    User userModified;

    @OneToMany (mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    @OneToMany (mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    List<Review> reviewList;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    Category category;
}
