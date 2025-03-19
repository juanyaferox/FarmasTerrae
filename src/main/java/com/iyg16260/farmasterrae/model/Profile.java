package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String type;

    String description;

    @OneToMany (mappedBy = "profile", fetch = FetchType.LAZY)
    List<User> userList;
}
