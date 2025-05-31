package com.iyg16260.farmasterrae.spec;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecification {

    /**
     * {@code Like} en todos los campo string y de los productos relacionados
     */
    public Specification<Order> searchLike(String keyword) {
        return GenericSpecification.likeIgnoreCase(new String[]{"address", "name",
                "orderDetails.product.reference", "orderDetails.product.name", "orderDetails.product.description"}, keyword);
    }

    // Coincidencia en sale status

    /**
     * {@code Equal (=) } con SaleStatus
     */
    public Specification<Order> searchBySaleStatus(SaleStatus saleStatus) {
        return GenericSpecification.equal("status", saleStatus);
    }

    /**
     * {@code Like} con SaleStatus
     */
    public Specification<Order> searchLikeSaleStatus(String keyword) {
        return SaleStatus.findByValueContainsIgnoreCase(keyword).stream()
                .map(this::searchBySaleStatus)
                .reduce(Specification::or).orElse(null);
    }

    // Coincidencia en payment method

    /**
     * {@code Equal (=) } con PaymentMethod
     */
    public Specification<Order> searchByPaymentMethod(PaymentMethod paymentMethod) {
        return GenericSpecification.equal("paymentMethod", paymentMethod);
    }

    /**
     * {@code Like} con PaymentMethod
     */
    public Specification<Order> searchLikePaymentMethod(String keyword) {
        return PaymentMethod.findByValueContainsIgnoreCase(keyword).stream()
                .map(this::searchByPaymentMethod)
                .reduce(Specification::or).orElse(null);
    }
}
