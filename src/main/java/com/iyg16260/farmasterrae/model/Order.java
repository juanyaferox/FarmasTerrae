package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.SaleEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Double totalPrice;

    @Enumerated (EnumType.ORDINAL)
    SaleEnum.SaleStatus status;

    @Enumerated (EnumType.ORDINAL)
    SaleEnum.PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userId")
    User user;

    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY)
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
