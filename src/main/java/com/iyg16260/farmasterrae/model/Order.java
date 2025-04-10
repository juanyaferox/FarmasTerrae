package com.iyg16260.farmasterrae.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.json.PaymentDetails;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Double totalPrice;

    @Enumerated (EnumType.ORDINAL)
    SaleEnum.SaleStatus status;

    @Column(columnDefinition = "json")
    String paymentDetailsJson;

    @Transient
    PaymentDetails paymentDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userId")
    User user;

    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @ToString.Exclude
    List<OrderDetails> orderDetails;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

    @PrePersist
    @PreUpdate
    void convertPaymentDetailsToJson() throws JsonProcessingException {
        if (paymentDetails != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            this.paymentDetailsJson = objectMapper.writeValueAsString(paymentDetails);
        }
    }

    @PostLoad
    void convertJsonToPaymentDetails() throws IOException {
        if (paymentDetailsJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            this.paymentDetails = objectMapper.readValue(paymentDetailsJson, PaymentDetails.class);
        }
    }
}
