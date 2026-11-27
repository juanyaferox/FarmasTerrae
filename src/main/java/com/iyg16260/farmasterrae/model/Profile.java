package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table (name = "profiles")
public class Profile implements GrantedAuthority, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Column (unique = true)
    String type;

    String description;

    @OneToMany (mappedBy = "profile", fetch = FetchType.LAZY)
    @ToString.Exclude
    List<User> userList;

    @Override
    public String getAuthority() {
        return type;
    }

    /**
     * Igualdad por id: un perfil sin id (todavía no guardado) solo es igual a sí mismo. Nunca compara
     * asociaciones, así que no inicializa relaciones lazy ni entra en ciclos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Profile other)) return false;
        Long currentId = getId();
        return currentId != null && currentId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Profile.class.hashCode();
    }
}
