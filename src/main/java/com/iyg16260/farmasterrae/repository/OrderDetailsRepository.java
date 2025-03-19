package com.iyg16260.farmasterrae.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.OrderDetailsId;

public interface OrderDetailsRepository extends JpaRepository <OrderDetails, OrderDetailsId> {

}
