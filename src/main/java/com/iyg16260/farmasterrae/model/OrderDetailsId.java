package com.iyg16260.farmasterrae.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class OrderDetailsId implements Serializable {
    
    Long idOrder;
    Long idProduct;
}
