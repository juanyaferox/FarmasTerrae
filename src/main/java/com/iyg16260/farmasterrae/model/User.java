package com.iyg16260.farmasterrae.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Entity
@Getter
@Setter
@ToString
@Table (name = "users")
@SQLRestriction("deletedAt IS NULL")
// Los UPDATE solo incluyen las columnas modificadas: un cambio de contraseña que leyó la cuenta antes de que
// se eliminara no puede reescribir deletedAt, username ni email con los valores antiguos y "resucitarla"
@DynamicUpdate
public class User implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Column (unique = true)
    String username;

    String fullName;

    @PrePersist
    @PreUpdate
    private void preSave() throws ResponseStatusException {
        if (this.deletedAt == null) {
            this.username = normalizeAndValidate(this.username);
        }
    }

    @ToString.Exclude
    String password;

    @Column (unique = true)
    String email;

    String phone;

    String address;

    LocalDateTime deletedAt;

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn (name = "profileId", nullable = false)
    @ToString.Exclude
    Profile profile;

    @OneToMany (mappedBy = "userModified", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JsonIgnore
    @ToString.Exclude
    List<Product> productList;

    @OneToMany (mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    List<Order> orderList;

    @OneToMany (mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    List<Review> reviewList;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + profile.getType()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return this.deletedAt == null;
    }

    /**
     * Dos usuarios son iguales si tienen el mismo id: Spring Security usa el usuario autenticado como clave del
     * {@code SessionRegistry}, así que la misma cuenta cargada en dos peticiones distintas tiene que ser el mismo
     * principal. Un usuario sin id (todavía no guardado) solo es igual a sí mismo. Nunca compara asociaciones.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        Long currentId = getId();
        return currentId != null && currentId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }

    private String normalizeAndValidate(String input) throws ResponseStatusException {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario no puede ser nulo");
        }
        input = input.trim();
        if (!input.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre de usuario debe tener entre 3 y 20 caracteres alfanuméricos sin espacios.");
        }
        return input.toLowerCase(Locale.ROOT);
    }
}
