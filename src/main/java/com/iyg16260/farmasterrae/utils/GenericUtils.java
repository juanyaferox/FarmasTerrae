package com.iyg16260.farmasterrae.utils;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

public class GenericUtils {
    /**
     * Comprueba si un DNI es válido
     * @param dni DNI a comprobar
     * @return true si es válido, false en caso contrario
     */
    @SuppressWarnings("unused")
    private static boolean isDNIValid(String dni) {
        if (dni == null || !dni.matches("\\d{8}[A-Z]")) {
            return false;
        }
        int numeroDNI = Integer.parseInt(dni.substring(0, 8));
        char letraDNI = dni.charAt(8);
        String letras_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";
        char letraCorrecta = letras_DNI.charAt(numeroDNI % 23);

        return letraDNI == letraCorrecta;
    }

    public static <T>T mapper(Object object, Class<T> targetClass) {
        try {
            T targetObject = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(object, targetObject);
            return targetObject;
        } catch (Exception e) {
            return null;
        }
    }

    public static OrderDetailsDTO getOrderDetailsDTO(Order order) {
        var orderDTO = new OrderDetailsDTO();
        orderDTO.setCreatedAt(order.getCreatedAt().format(new DateTimeFormatterBuilder().toFormatter().localizedBy(Locale.getDefault())));
        orderDTO.setUpdatedAt(order.getUpdatedAt().format(new DateTimeFormatterBuilder().toFormatter().localizedBy(Locale.getDefault())));
        orderDTO.setPaymentMethod(order.getPaymentDetails()!=null ? order.getPaymentDetails().getMethod() : null);
        orderDTO.setStatus(order.getStatus()!=null ? order.getStatus().getValue(): null);
        orderDTO.setTotalPrice(order.getTotalPrice());
        Map<ProductDTO, Integer> productDTOIntegerMap = new HashMap<>();

        order.getOrderDetails().stream().forEach(o -> {
            var productDTO = new ProductDTO();
            BeanUtils.copyProperties(o.getProduct(),productDTO);
            productDTOIntegerMap.put(productDTO,o.getAmount());
        });
        orderDTO.setProducts(productDTOIntegerMap);

        return orderDTO;
    }
}
