package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@Table (name = "orderDetails")
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

    @ToString.Exclude
    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "idOrder")
    @MapsId ("idOrder")
    Order order;

    @ToString.Exclude
    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "idProduct")
    @MapsId ("idProduct")
    Product product;

    public OrderDetails(Order order, Product product) {
        this.order = order;
        this.product = product;
        this.id = new OrderDetailsId(order.getId(), product.getId());
    }
}
