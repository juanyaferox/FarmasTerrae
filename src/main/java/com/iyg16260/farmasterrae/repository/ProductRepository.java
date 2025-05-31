package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByReference(String reference);

    Page<Product> findByCategory(Category category, Pageable pageable);

    boolean existsByReference(String reference);
}
