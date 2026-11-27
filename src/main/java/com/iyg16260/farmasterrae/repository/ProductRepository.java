package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByReference(String reference);

    Optional<Product> findByReferenceAndDeletedAtIsNull(String reference);

    boolean existsByReference(String reference);

    @Modifying (clearAutomatically = true, flushAutomatically = true)
    @Query ("UPDATE Product p SET p.stock = p.stock - :qty, p.version = COALESCE(p.version, 0) + 1 WHERE p.reference = :reference AND p.deletedAt IS NULL AND p.stock >= :qty")
    int decrementStock(@Param ("reference") String reference, @Param ("qty") int qty);

    @Modifying (clearAutomatically = true, flushAutomatically = true)
    @Query ("UPDATE Product p SET p.stock = p.stock + :qty, p.version = COALESCE(p.version, 0) + 1 WHERE p.reference = :reference")
    int incrementStock(@Param ("reference") String reference, @Param ("qty") int qty);

    @Query (value = "SELECT * FROM products WHERE deletedAt IS NULL ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Product> findRandomProducts(@Param ("count") int count);

}
