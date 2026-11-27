package com.iyg16260.farmasterrae.mapper;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.OrderItemDTO;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.service.S3StorageService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Mapper (componentModel = "spring")
public abstract class OrderMapper {

    private static final DateTimeFormatter DATE_DMY_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId MADRID_ZONE = ZoneId.of("Europe/Madrid");

    @Autowired
    S3StorageService s3StorageService;

    @Mapping (target = "items", ignore = true)
    @Mapping (target = "status", source = "status", qualifiedByName = "formatStatus")
    @Mapping (target = "paymentMethod", source = "paymentMethod", qualifiedByName = "formatPaymentMethod")
    @Mapping (target = "createdAt", source = "createdAt", qualifiedByName = "formatDateDMY")
    @Mapping (target = "updatedAt", source = "updatedAt", qualifiedByName = "formatDateDMY")
    public abstract OrderDetailsDTO orderToOrderDetailsDTO(Order order);

    @Mapping (target = "createdAt", source = "createdAt", qualifiedByName = "formatDateDMY")
    @Mapping (target = "updatedAt", source = "updatedAt", qualifiedByName = "formatDateDMY")
    public abstract OrderDTO orderToOrderDTO(Order order);

    @AfterMapping
    void setItems(Order order, @MappingTarget OrderDetailsDTO dto) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            dto.setItems(List.of());
            return;
        }
        List<OrderItemDTO> items = order.getOrderDetails().stream()
                .sorted(Comparator.comparing(
                        od -> od.getProduct() != null && od.getProduct().getReference() != null
                                ? od.getProduct().getReference() : "",
                        Comparator.naturalOrder()))
                .map(this::toOrderItemDTO)
                .toList();
        dto.setItems(items);
    }

    private OrderItemDTO toOrderItemDTO(OrderDetails od) {
        Product product = od.getProduct();
        String productReference = product != null ? product.getReference() : null;
        String productName = od.getProductName() != null
                ? od.getProductName()
                : (product != null ? product.getName() : null);
        String imagePath = product != null ? s3StorageService.resolveImageUrl(product.getImagePath()) : null;
        Integer unitPriceCents = od.getUnitPriceCents() != null
                ? od.getUnitPriceCents()
                : (product != null ? product.getPrice() : null);
        BigDecimal unitPrice = unitPriceCents != null
                ? BigDecimal.valueOf(unitPriceCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : null;
        Integer amount = od.getAmount();
        BigDecimal subtotal = (unitPrice != null && amount != null)
                ? unitPrice.multiply(BigDecimal.valueOf(amount)).setScale(2, RoundingMode.HALF_UP)
                : null;
        return new OrderItemDTO(productReference, productName, imagePath, unitPrice, amount, subtotal);
    }

    @Named ("formatStatus")
    String formatStatus(SaleStatus status) {
        return status != null ? status.getValue() : null;
    }

    @Named ("formatPaymentMethod")
    String formatPaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethod != null ? paymentMethod.getValue() : null;
    }

    @Named ("formatDateDMY")
    String formatDateDMY(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(MADRID_ZONE)
                .format(DATE_DMY_FORMATTER);
    }
}
