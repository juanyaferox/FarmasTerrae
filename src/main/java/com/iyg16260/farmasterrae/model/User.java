package com.iyg16260.farmasterrae.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true)
    String username;

    String password;

    @Column(unique = true)
    String email;

    String phone;

    String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="profileId")
    Profile profile;

    @OneToMany(mappedBy = "userModified",fetch = FetchType.LAZY)
    @JsonIgnore
    List<Product> productList;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    List<Order> orderList;

    @OneToMany(mappedBy = "user")
    List<Review> reviewList;
}
