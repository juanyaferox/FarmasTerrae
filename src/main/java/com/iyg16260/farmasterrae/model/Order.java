package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table (name = "orders")
public class Order {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    Double totalPrice;

    @Enumerated (EnumType.STRING)
    SaleStatus status;

    @Enumerated (EnumType.STRING)
    PaymentMethod paymentMethod;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "userId")
    User user;

    @OneToMany (mappedBy = "order", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

}
