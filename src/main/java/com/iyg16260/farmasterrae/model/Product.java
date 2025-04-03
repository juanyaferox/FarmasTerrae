package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    String description;

    String imagePath;

    @PrePersist
    void prePersist() {
        if (imagePath == null || imagePath.isEmpty()) {
            //imagePath = 1;//ruta de imagen x defecto
        }
    }

    Double price;

    Integer stock; // puede que se pueda sacar del order

    @Column(unique = true)
    String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userModifiedId")
    User userModified;

    @OneToMany(mappedBy = "product",fetch = FetchType.LAZY)
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    List<Review> reviewList;
}
