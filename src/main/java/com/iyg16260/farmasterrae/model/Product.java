package com.iyg16260.farmasterrae.model;

import com.iyg16260.farmasterrae.enums.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@DynamicUpdate
@Table (name = "products", indexes = {@Index (name = "idxProductCategory", columnList = "category"),
        @Index (name = "idxProductReference", columnList = "reference")})
public class Product {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Version
    @ColumnDefault ("0")
    private Long version;

    String name;

    @Column (length = 1500)
    String description;

    String imagePath;

    Integer price;

    Integer stock;

    LocalDateTime deletedAt;

    @Column (unique = true)
    String reference;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "userModifiedId")
    @ToString.Exclude
    User userModified;

    @OneToMany (mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    @OneToMany (mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    List<Review> reviewList;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    Category category;

    /**
     * Igualdad por id: un producto sin id (todavía no guardado) solo es igual a sí mismo. Nunca compara
     * asociaciones, así que no inicializa relaciones lazy ni entra en ciclos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        Long currentId = getId();
        return currentId != null && currentId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Product.class.hashCode();
    }
}
