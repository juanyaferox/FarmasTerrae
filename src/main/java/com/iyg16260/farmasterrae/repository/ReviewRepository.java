package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.Review;
import com.iyg16260.farmasterrae.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByUser(User user, Pageable pageable);
}
