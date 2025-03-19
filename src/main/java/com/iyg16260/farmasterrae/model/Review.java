package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String title;
    String content;
    Integer scoring;

    @ManyToOne(fetch = FetchType.LAZY)
    Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    User user;
}
