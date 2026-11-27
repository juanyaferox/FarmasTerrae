package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Page<Order> findByUser(User user, Pageable pageable);

    List<Order> findByUser(User user);

    List<Order> findByUserAndStatus(User user, SaleStatus status);

    Optional<Order> findByIdAndUser_Id(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o SET o.status = :newStatus, o.updatedAt = :updatedAt WHERE o.id = :id AND o.status = :expectedStatus")
    int transitionStatus(@Param("id") Long id,
                         @Param("expectedStatus") SaleStatus expectedStatus,
                         @Param("newStatus") SaleStatus newStatus,
                         @Param("updatedAt") LocalDateTime updatedAt);

}
