package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
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
    @JoinColumn(name="orderId")
    @MapsId("orderId")
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="productId")
    @MapsId("productId")
    Product product;
}
