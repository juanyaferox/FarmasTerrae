package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table (name = "orders")
public class Order {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    BigDecimal totalPrice;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    SaleStatus status;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    PaymentMethod paymentMethod;

    String name;
    String address;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "userId")
    @ToString.Exclude
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

    /**
     * Igualdad por id: un pedido sin id (todavía no guardado) solo es igual a sí mismo. Nunca compara
     * asociaciones, así que no inicializa relaciones lazy ni entra en ciclos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        Long currentId = getId();
        return currentId != null && currentId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Order.class.hashCode();
    }

}
