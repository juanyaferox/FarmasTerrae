package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table (name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_reviews_user_product", columnNames = {"userId", "productId"}))
public class Review {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Column (length = 50)
    String title;

    @Column (length = 1000)
    String content;

    Integer scoring;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "productId")
    @ToString.Exclude
    Product product;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "userId")
    @ToString.Exclude
    User user;

    /**
     * Igualdad por id: una reseña sin id (todavía no guardada) solo es igual a sí misma. Nunca compara
     * asociaciones, así que no inicializa relaciones lazy ni entra en ciclos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review other)) return false;
        Long currentId = getId();
        return currentId != null && currentId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Review.class.hashCode();
    }
}
