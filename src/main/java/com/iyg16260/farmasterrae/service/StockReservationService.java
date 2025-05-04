package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockReservationService {

    private static final String RESERVATION_ID_KEY = "stockReservationId";
    private static final String RESERVATION_TIME_KEY = "reservationTime";

    @Value ("${stock.reservation.timeout.minutes:15}")
    private int reservationTimeoutMinutes;

    @Autowired
    private ProductRepository productRepository;

    // Mapa para almacenar reservas: reservationId -> Map<productReference, cantidadReservada>
    private final ConcurrentHashMap<String, Map<String, Integer>> stockReservations = new ConcurrentHashMap<>();
    // Mapa para seguir los tiempos de reserva: reservationId -> marca de tiempo
    private final ConcurrentHashMap<String, LocalDateTime> reservationTimes = new ConcurrentHashMap<>();

    /**
     * Crea o actualiza una reserva de stock para los productos en el carrito
     *
     * @param session La sesión HTTP que contiene el carrito
     * @param cart    El carrito con los productos
     * @return true si la reserva fue exitosa, false si algún producto no tiene suficiente stock
     */
    @Transactional
    public boolean reserveStockForCart(HttpSession session, SessionCart cart) {
        // Obtener o crear ID de reserva para esta sesión
        String reservationId = getOrCreateReservationId(session);

        // Liberar reservas previas para esta sesión
        releaseReservation(reservationId);

        // Crear nueva reserva
        Map<String, Integer> reservedProducts = new HashMap<>();

        // Comprobar si hay suficiente stock para todos los productos en el carrito
        for (Map.Entry<String, Integer> entry : cart.getProducts().entrySet()) {
            String productRef = entry.getKey();
            int requestedAmount = entry.getValue();

            Product product = productRepository.findByReference(productRef)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productRef));

            // Obtener stock disponible actual (stock real menos lo ya reservado)
            int availableStock = getAvailableStock(product, reservationId);

            if (availableStock < requestedAmount) {
                return false; // No hay suficiente stock
            }

            reservedProducts.put(productRef, requestedAmount);
        }

        // Si llegamos aquí, tenemos suficiente stock para todos los productos, así que guardamos la reserva
        stockReservations.put(reservationId, reservedProducts);
        reservationTimes.put(reservationId, LocalDateTime.now());
        session.setAttribute(RESERVATION_TIME_KEY, LocalDateTime.now());

        return true;
    }

    /**
     * Obtiene el stock disponible para un producto considerando las reservas actuales
     */
    public int getAvailableStock(Product product, String excludeReservationId) {
        int reservedStock = 0;

        // Calcular el stock total reservado para este producto en todas las reservas
        // excepto la reserva de la sesión actual
        for (Map.Entry<String, Map<String, Integer>> reservation : stockReservations.entrySet()) {
            String reservationId = reservation.getKey();
            if (!reservationId.equals(excludeReservationId)) {
                Map<String, Integer> products = reservation.getValue();
                reservedStock += products.getOrDefault(product.getReference(), 0);
            }
        }

        return product.getStock() - reservedStock;
    }

    /**
     * Confirma una reserva y actualiza el stock real del producto
     */
    @Transactional
    public boolean confirmReservation(String reservationId) {
        Map<String, Integer> reservation = stockReservations.get(reservationId);
        if (reservation == null) {
            return false;
        }

        // Actualizar el stock real del producto en la base de datos
        for (Map.Entry<String, Integer> entry : reservation.entrySet()) {
            String productRef = entry.getKey();
            int reservedAmount = entry.getValue();

            Product product = productRepository.findByReference(productRef)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productRef));

            // Verificar nuevamente que tenemos suficiente stock
            if (product.getStock() < reservedAmount) {
                throw new RuntimeException("No hay suficiente stock para el producto: " + productRef);
            }

            product.setStock(product.getStock() - reservedAmount);
            productRepository.save(product);
        }

        // Eliminar la reserva
        stockReservations.remove(reservationId);
        reservationTimes.remove(reservationId);

        return true;
    }

    /**
     * Libera una reserva sin actualizar el stock del producto
     */
    public void releaseReservation(String reservationId) {
        stockReservations.remove(reservationId);
        reservationTimes.remove(reservationId);
    }

    /**
     * Obtiene o crea un ID de reserva para la sesión
     */
    private String getOrCreateReservationId(HttpSession session) {
        String reservationId = (String) session.getAttribute(RESERVATION_ID_KEY);
        if (reservationId == null) {
            reservationId = UUID.randomUUID().toString();
            session.setAttribute(RESERVATION_ID_KEY, reservationId);
        }
        return reservationId;
    }

    /**
     * Obtiene el ID de reserva de la sesión
     */
    public String getReservationId(HttpSession session) {
        return (String) session.getAttribute(RESERVATION_ID_KEY);
    }

    /**
     * Tarea programada para limpiar reservas expiradas
     * Se ejecuta cada minuto
     */
    @Scheduled (fixedRate = 60000)
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        reservationTimes.forEach((reservationId, timestamp) -> {
            if (timestamp.plusMinutes(reservationTimeoutMinutes).isBefore(now)) {
                // Esta reserva ha expirado
                releaseReservation(reservationId);
            }
        });
    }

    /**
     * Comprueba si la reserva de una sesión ha expirado
     */
    public boolean isReservationExpired(HttpSession session) {
        LocalDateTime reservationTime = (LocalDateTime) session.getAttribute(RESERVATION_TIME_KEY);
        if (reservationTime == null) {
            return true;
        }

        return reservationTime.plusMinutes(reservationTimeoutMinutes).isBefore(LocalDateTime.now());
    }
}