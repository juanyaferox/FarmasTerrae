package com.iyg16260.farmasterrae.dto.response;

import com.iyg16260.farmasterrae.dto.payment.PaymentDetailsDTO;
import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;

import java.util.List;

/**
 * Versión inicial del pedido antes de confirmarlo
 *
 * @param products       productos del carrito con su cantidad
 * @param paymentMethods métodos de pago disponibles
 * @param paymentDetails detalles del pago precargados con los datos del usuario
 */
public record CheckoutResponseDTO(List<ProductQuantityDTO> products,
                                  List<EnumOptionDTO> paymentMethods,
                                  PaymentDetailsDTO paymentDetails) {
}
