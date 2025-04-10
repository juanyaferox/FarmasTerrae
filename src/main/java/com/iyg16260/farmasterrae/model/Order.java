package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.json.PaymentDetails;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
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

    @JdbcTypeCode (SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private PaymentDetails paymentDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userId")
    User user;

    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(Clock.systemUTC());
    }
}
