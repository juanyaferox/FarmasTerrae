package com.iyg16260.farmasterrae.mapper.user;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.mapper.products.ProductMapper;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import org.mapstruct.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface OrderMapper {

    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "paymentMethod", source = "paymentDetails.method")
    OrderDetailsDTO orderToOrderDetailsDTO(Order order, @Context ProductMapper productMapper);

    @AfterMapping
    default void setProducts(Order order, @MappingTarget OrderDetailsDTO dto, @Context ProductMapper productMapper) {
        Map<ProductDTO, Integer> map = new HashMap<>();
        for (OrderDetails od : order.getOrderDetails()) {
            ProductDTO productDTO = productMapper.productToProductDTO(od.getProduct());
            map.put(productDTO, od.getAmount());
        }
        dto.setProducts(map);
    }
}