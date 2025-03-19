package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.iyg16260.farmasterrae.model.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository <Order, Long> {
    Page<Order> findByUser(User user, Pageable pageable);

}
