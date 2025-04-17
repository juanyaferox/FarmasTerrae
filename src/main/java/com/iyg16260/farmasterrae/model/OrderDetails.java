package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "orderDetails")
public class OrderDetails {

    @EmbeddedId
    OrderDetailsId id;

    Integer amount;

    @PrePersist
    void prePersist() {
        if (amount == null) {
            amount = 1;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="idOrder")
    @MapsId("idOrder")
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="idProduct")
    @MapsId("idProduct")
    Product product;
}
