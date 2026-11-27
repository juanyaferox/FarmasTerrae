package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = "product")
    Page<Review> findByUser(User user, Pageable pageable);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    @Query("SELECT r.product.id FROM Review r WHERE r.user.id = :userId")
    Set<Long> findReviewedProductIdsByUserId(@Param("userId") Long userId);
}
