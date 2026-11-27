package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockReservationService {

    public static final String RESERVATION_ID_KEY = "stockReservationId";
    public static final String RESERVATION_TIME_KEY = "reservationTime";

    @Value ("${stock.reservation.timeout.minutes:15}")
    private int reservationTimeoutMinutes;

    @Value ("${stock.reservation.max-per-product:20}")
    private int maxReservedPerProduct;

    @Autowired
    private ProductRepository productRepository;

    private Clock clock = Clock.systemUTC();

    private final Object reservationLock = new Object();

    // Mapa para almacenar reservas: reservationId -> Map<productReference, cantidadReservada>
    private final ConcurrentHashMap<String, Map<String, Integer>> stockReservations = new ConcurrentHashMap<>();
    // Mapa para seguir los tiempos de reserva: reservationId -> marca de tiempo
    private final ConcurrentHashMap<String, LocalDateTime> reservationTimes = new ConcurrentHashMap<>();

    void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /**
     * Crea o actualiza una reserva de stock para los productos en el carrito.
     * Si algún producto no cabe o excede el límite por sesión, conserva intacta cualquier reserva previa.
     *
     * @param session La sesión HTTP que contiene el carrito
     * @param cart    El carrito con los productos
     * @return true si la reserva fue exitosa, false si algún producto no tiene suficiente stock o excede el límite
     */
    @Transactional (readOnly = true)
    public boolean reserveStockForCart(HttpSession session, SessionCart cart) {
        Map<String, Integer> cartProducts = cart != null ? cart.getProducts() : Map.of();

        synchronized (reservationLock) {
            String existingReservationId = getReservationId(session);
            if (cartProducts.isEmpty()) {
                if (existingReservationId != null) {
                    stockReservations.remove(existingReservationId);
                    reservationTimes.remove(existingReservationId);
                }
                session.removeAttribute(RESERVATION_TIME_KEY);
                return true;
            }

            String reservationId = existingReservationId != null ? existingReservationId : UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now(clock);
            Map<String, Integer> candidateReservation = new HashMap<>();

            for (Map.Entry<String, Integer> entry : cartProducts.entrySet()) {
                String productRef = entry.getKey();
                Integer requestedObj = entry.getValue();
                if (requestedObj == null || requestedObj <= 0 || requestedObj > maxReservedPerProduct) {
                    return false;
                }
                int requestedAmount = requestedObj;

                Product product = productRepository.findByReference(productRef)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

                int availableStock = getAvailableStockLocked(product, reservationId, now);
                if (availableStock < requestedAmount) {
                    return false;
                }

                candidateReservation.put(productRef, requestedAmount);
            }

            if (existingReservationId == null) {
                session.setAttribute(RESERVATION_ID_KEY, reservationId);
            }
            stockReservations.put(reservationId, Map.copyOf(candidateReservation));
            reservationTimes.put(reservationId, now);
            session.setAttribute(RESERVATION_TIME_KEY, now);
            return true;
        }
    }

    /**
     * Obtiene el stock disponible para un producto considerando únicamente las reservas vigentes (no expiradas)
     *
     * @param product              producto a consultar
     * @param excludeReservationId id de reserva a excluir del cálculo
     * @return stock disponible actual
     */
    public int getAvailableStock(Product product, String excludeReservationId) {
        synchronized (reservationLock) {
            return getAvailableStockLocked(product, excludeReservationId, LocalDateTime.now(clock));
        }
    }

    private int getAvailableStockLocked(Product product, String excludeReservationId, LocalDateTime now) {
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        int reservedStock = 0;

        for (Map.Entry<String, Map<String, Integer>> reservation : stockReservations.entrySet()) {
            String reservationId = reservation.getKey();
            if (reservationId.equals(excludeReservationId)) {
                continue;
            }
            if (isExpiredLocked(reservationId, now)) {
                continue;
            }
            Map<String, Integer> products = reservation.getValue();
            reservedStock += products.getOrDefault(product.getReference(), 0);
        }

        return Math.max(0, currentStock - reservedStock);
    }

    /**
     * Comprueba si la reserva de la sesión está vigente, existe en memoria y coincide exactamente con el carrito
     *
     * @param session sesión HTTP
     * @param cart    carrito actual de la sesión
     * @return true si la reserva es válida y coincide con el carrito
     */
    public boolean isReservationValid(HttpSession session, SessionCart cart) {
        if (session == null || cart == null) {
            return false;
        }
        Map<String, Integer> cartProducts = cart.getProducts();
        if (cartProducts.isEmpty()) {
            return false;
        }
        synchronized (reservationLock) {
            String reservationId = getReservationId(session);
            if (reservationId == null) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime sessionTime = (LocalDateTime) session.getAttribute(RESERVATION_TIME_KEY);
            if (sessionTime == null || !sessionTime.plusMinutes(reservationTimeoutMinutes).isAfter(now)) {
                return false;
            }
            if (isExpiredLocked(reservationId, now)) {
                return false;
            }
            Map<String, Integer> reserved = stockReservations.get(reservationId);
            return reserved != null && reserved.equals(cartProducts);
        }
    }

    /**
     * Libera una reserva tras el commit de la transacción actual (o inmediatamente si no hay transacción activa)
     *
     * @param reservationId id de la reserva a liberar
     */
    public void releaseReservationAfterCommit(String reservationId) {
        if (reservationId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    releaseReservation(reservationId);
                }
            });
        } else {
            releaseReservation(reservationId);
        }
    }

    /**
     * Libera una reserva sin actualizar el stock del producto
     *
     * @param reservationId id de la reserva a liberar
     */
    public void releaseReservation(String reservationId) {
        if (reservationId == null) {
            return;
        }
        synchronized (reservationLock) {
            stockReservations.remove(reservationId);
            reservationTimes.remove(reservationId);
        }
    }

    /**
     * Limpia los atributos de reserva almacenados en la sesión HTTP
     *
     * @param session sesión HTTP
     */
    public void clearSessionReservationAttributes(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(RESERVATION_ID_KEY);
        session.removeAttribute(RESERVATION_TIME_KEY);
    }

    /**
     * Obtiene el id de reserva de la sesión
     *
     * @param session sesión HTTP
     * @return id de reserva o null si no existe
     */
    public String getReservationId(HttpSession session) {
        return session != null ? (String) session.getAttribute(RESERVATION_ID_KEY) : null;
    }

    /**
     * Tarea programada para limpiar reservas expiradas
     * Se ejecuta cada minuto
     */
    @Scheduled (fixedRate = 60000)
    public void cleanupExpiredReservations() {
        synchronized (reservationLock) {
            LocalDateTime now = LocalDateTime.now(clock);
            List<String> expiredIds = new ArrayList<>();
            for (Map.Entry<String, LocalDateTime> entry : reservationTimes.entrySet()) {
                if (entry.getValue() == null || !entry.getValue().plusMinutes(reservationTimeoutMinutes).isAfter(now)) {
                    expiredIds.add(entry.getKey());
                }
            }
            for (String expiredId : expiredIds) {
                stockReservations.remove(expiredId);
                reservationTimes.remove(expiredId);
            }
        }
    }

    /**
     * Comprueba si la reserva de una sesión ha expirado o no existe en memoria
     *
     * @param session sesión HTTP a verificar
     * @return true si la reserva ha expirado o no existe, false si está vigente
     */
    public boolean isReservationExpired(HttpSession session) {
        if (session == null) {
            return true;
        }
        synchronized (reservationLock) {
            String reservationId = getReservationId(session);
            if (reservationId == null) {
                return true;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime reservationTime = (LocalDateTime) session.getAttribute(RESERVATION_TIME_KEY);
            if (reservationTime == null || !reservationTime.plusMinutes(reservationTimeoutMinutes).isAfter(now)) {
                return true;
            }
            return isExpiredLocked(reservationId, now) || !stockReservations.containsKey(reservationId);
        }
    }

    Map<String, Integer> getReservationSnapshot(String reservationId) {
        if (reservationId == null) {
            return null;
        }
        synchronized (reservationLock) {
            Map<String, Integer> reserved = stockReservations.get(reservationId);
            return reserved != null ? Map.copyOf(reserved) : null;
        }
    }

    private boolean isExpiredLocked(String reservationId, LocalDateTime now) {
        LocalDateTime timestamp = reservationTimes.get(reservationId);
        return timestamp == null || !timestamp.plusMinutes(reservationTimeoutMinutes).isAfter(now);
    }
}
