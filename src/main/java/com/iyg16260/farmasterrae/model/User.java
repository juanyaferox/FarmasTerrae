package com.iyg16260.farmasterrae.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Where;
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

@Entity
@Data
@Table (name = "users")
@Where (clause = "deletedAt IS NULL") // Deprecated, pero más sencillo
public class User implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Column (unique = true)
    String username;

    @PrePersist
    @PreUpdate
    private void preSave() throws ResponseStatusException {
        if (this.deletedAt == null) {
            this.username = normalizeAndValidate(this.username);
        }
    }

    String password;

    @Column (unique = true)
    String email;

    String phone;

    String address;

    LocalDateTime deletedAt;

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn (name = "profileId", nullable = false)
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
        return UserDetails.super.isEnabled();
    }

    private String normalizeAndValidate(String input) throws ResponseStatusException {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario no puede ser nulo");
        }
        if (!input.matches("^[a-zA-Z0-9]{3,20}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre de usuario debe tener entre 3 y 20 caracteres alfanuméricos sin espacios.");
        }
        return input.toLowerCase();
    }

    public String getNormalizedUsername() {
        return username != null ? username.toLowerCase() : null;
    }
}
