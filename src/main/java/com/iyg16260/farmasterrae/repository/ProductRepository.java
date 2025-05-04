package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByReference(String reference);

    Page<Product> findByCategory(Category category, Pageable pageable);

    boolean existsByReference(String reference);
}
