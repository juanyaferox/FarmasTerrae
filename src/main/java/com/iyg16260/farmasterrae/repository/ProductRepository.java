package com.iyg16260.farmasterrae.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iyg16260.farmasterrae.model.Product;

public interface ProductRepository extends JpaRepository <Product, Long> {

}
