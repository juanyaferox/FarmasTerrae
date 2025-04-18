package com.iyg16260.farmasterrae.mapper.user;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.mapper.products.ProductMapper;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public abstract class OrderMapper {

    @Autowired
    ProductMapper productMapper;

    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    public abstract OrderDetailsDTO orderToOrderDetailsDTO(Order order);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateDMY")
    @Mapping(target = "status", source = "status.value")
    public abstract OrderDTO orderToOrderDTO(Order order);

    @AfterMapping
    void setProducts(Order order, @MappingTarget OrderDetailsDTO dto) {
        Map<ProductDTO, Integer> map = new HashMap<>();
        for (OrderDetails od : order.getOrderDetails()) {
            ProductDTO productDTO = productMapper.productToProductDTO(od.getProduct());
            map.put(productDTO, od.getAmount());
        }
        dto.setProducts(map);
    }

    @Named("formatDateDMY")
    String formatDateDMY(LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : null;
    }

    @Named("formatDateDMYHM")
    String formatDateDMYHM(LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : null;
    }



}