package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table (name = "orderDetails")
public class OrderDetails {

    @EmbeddedId
    OrderDetailsId id;

    Integer amount;

    @Column (nullable = true)
    Integer unitPriceCents;

    @Column (nullable = true)
    String productName;

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

    /**
     * Igualdad por la clave compuesta (pedido, producto) cuando está completa; una línea sin clave completa solo
     * es igual a sí misma. Nunca compara el pedido ni el producto, así que no entra en el ciclo
     * {@code Order -> orderDetails -> order}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderDetails other)) return false;
        OrderDetailsId currentId = getId();
        return hasCompleteId(currentId) && currentId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return OrderDetails.class.hashCode();
    }

    private static boolean hasCompleteId(OrderDetailsId candidate) {
        return candidate != null && candidate.getIdOrder() != null && candidate.getIdProduct() != null;
    }
}
