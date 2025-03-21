package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@Table(name = "profiles")
public class Profile implements GrantedAuthority, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String type;

    String description;

    @OneToMany (mappedBy = "profile", fetch = FetchType.LAZY)
    List<User> userList;

    @Override
    public String getAuthority() {
        return type;
    }
}
