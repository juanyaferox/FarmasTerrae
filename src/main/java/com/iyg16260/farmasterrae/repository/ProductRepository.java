package com.iyg16260.farmasterrae.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iyg16260.farmasterrae.model.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository <Product, Long> {
    Optional<Product> findByReference(String reference);
}
