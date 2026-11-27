package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.payment.PaymentDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.mapper.OrderMapper;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.spec.OrderSpecification;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final int PAGE_SIZE_USER = 9;
    private final int PAGE_SIZE_ADMIN = 9;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    CartService cartService;

    @Autowired
    StockReservationService stockReservationService;

    /**
     * Página con los pedidos del usuario
     *
     * @param user usuario a obtener pedidos
     * @param page página actual
     * @return página con los pedidos actual
     */
    public Page<OrderDTO> getOrders(User user, int page, String sort, String dir) {

        Sort sortOrder = Sort.unsorted();
        if (sort != null) {
            Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortOrder = Sort.by(direction, sort);
        }

        return orderRepository.findByUser(user, PageRequest.of(page, PAGE_SIZE_USER, sortOrder))
                .map(orderMapper::orderToOrderDTO);
    }

    /**
     * Página con todos los pedidos
     *
     * @param page    página actual
     * @param keyword palabra a buscar
     * @return página de los pedidos actual
     */
    public Page<OrderDTO> getAllOrders(int page, String keyword, String sort, String dir) {

        Sort sortOrder = Sort.unsorted();
        if (sort != null) {
            Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortOrder = Sort.by(direction, sort);
        }

        OrderSpecification specUtil = new OrderSpecification();
        Specification<Order> spec = specUtil.searchLike(keyword)
                .or(specUtil.searchLikeSaleStatus(keyword))
                .or(specUtil.searchLikePaymentMethod(keyword));

        return orderRepository.findAll(spec, PageRequest.of(page, PAGE_SIZE_ADMIN, sortOrder))
                .map(orderMapper::orderToOrderDTO);
    }

    /**
     * Pedido por id si pertenece al usuario o si el usuario es admin.
     * Para usuarios no administradores devuelve siempre 404 (evita oráculo de existencia 403 vs 404).
     *
     * @param idUser  id del usuario
     * @param idOrder id del pedido
     * @return pedido coincidente
     * @throws ResponseStatusException NOT_FOUND si no existe o no pertenece al usuario
     */
    @Transactional (readOnly = true)
    public OrderDetailsDTO getOrder(long idUser, long idOrder) throws ResponseStatusException {
        User user = userService.getUser(idUser);

        Order order;
        if (Objects.equals(user.getProfile().getType(), "ADMIN")) {
            order = getOrderById(idOrder);
        } else {
            order = orderRepository.findByIdAndUser_Id(idOrder, idUser)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        }

        return orderMapper.orderToOrderDetailsDTO(order);
    }

    /**
     * Obtener pedido por id
     *
     * @param idOrder id del pedido
     * @return pedido coincidente
     * @throws ResponseStatusException si pedido no encontrado
     */
    private Order getOrderById(long idOrder) throws ResponseStatusException {
        return orderRepository.findById(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    private Order getOrderByIdForUpdate(long idOrder) throws ResponseStatusException {
        return orderRepository.findByIdForUpdate(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    /**
     * Guarda un pedido decrementando el stock atómicamente en base de datos y registrando instantáneas
     * de precio y nombre de producto en las líneas del pedido.
     *
     * @param user       usuario que realiza el pedido
     * @param cart       carrito con el pedido
     * @param saleStatus estado de venta
     * @param payment    detalles del pago
     * @param session    la sesión HTTP
     * @return pedido guardado
     */
    @Transactional
    public Order setOrder(User user, SessionCart cart,
                          SaleStatus saleStatus,
                          PaymentDetailsDTO payment,
                          HttpSession session) throws ResponseStatusException {
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay suficiente stock disponible o la reserva ha expirado. Por favor, revisa tu carrito.");
        }

        Map<String, Integer> rawProducts;
        String reservationId;
        synchronized (WebUtils.getSessionMutex(session)) {
            rawProducts = cart != null ? cart.getProducts() : Map.of();

            if (rawProducts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrito no encontrado");
            }

            if (!cartService.validateCartReservations(session)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay suficiente stock disponible o la reserva ha expirado. Por favor, revisa tu carrito.");
            }
            if (!rawProducts.equals(cartService.getCart(session).getProducts())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El carrito ha cambiado durante la confirmación. Por favor, revisa tu carrito.");
            }
            reservationId = stockReservationService.getReservationId(session);
        }

        // Ordenar por referencia para evitar deadlocks en actualizaciones concurrentes
        Map<String, Integer> sortedProducts = new TreeMap<>(rawProducts);

        for (Map.Entry<String, Integer> entry : sortedProducts.entrySet()) {
            String reference = entry.getKey();
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad de producto inválida.");
            }
            int updatedRows = productRepository.decrementStock(reference, quantity);
            if (updatedRows == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para completar el pedido.");
            }
        }

        List<Map.Entry<Product, Integer>> orderLines = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : sortedProducts.entrySet()) {
            Product product = productsService.getProductByReference(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            orderLines.add(Map.entry(product, quantity));
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(saleStatus);
        order.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));
        order.setPaymentMethod(payment.getPaymentMethod());
        order.setAddress(payment.getAddress());
        order.setName(payment.getFullName());
        Order savedOrder = orderRepository.save(order);

        List<OrderDetails> detailsList = new ArrayList<>();
        for (Map.Entry<Product, Integer> line : orderLines) {
            Product product = line.getKey();
            OrderDetails details = new OrderDetails(savedOrder, product);
            details.setAmount(line.getValue());
            details.setUnitPriceCents(product.getPrice());
            details.setProductName(product.getName());
            detailsList.add(details);
        }
        savedOrder.setOrderDetails(detailsList);
        orderRepository.flush();

        stockReservationService.releaseReservationAfterCommit(reservationId);

        return savedOrder;
    }

    /**
     * Borra por id de pedido y restaura el stock atómicamente si el pedido no estaba cancelado
     *
     * @param idOrder id del pedido
     * @throws ResponseStatusException si pedido no encontrado
     */
    @Transactional
    public void deleteOrderById(long idOrder) throws ResponseStatusException {
        Order order = getOrderByIdForUpdate(idOrder);
        List<Map.Entry<String, Integer>> itemsToRestore = List.of();
        if (order.getStatus() != SaleStatus.CANCELLED && order.getOrderDetails() != null) {
            itemsToRestore = extractRestorableLines(order);
        }
        orderRepository.delete(order);
        orderRepository.flush();
        restoreStockForLines(itemsToRestore);
    }

    /**
     * Actualiza el pedido cambiando el status según la máquina de estados permitida.
     * Al cancelar un pedido no cancelado previamente, restaura el stock atómicamente
     * bajo bloqueo pesimista y transición condicional para evitar restauraciones duplicadas.
     *
     * @param idOrder id del pedido a cambiar
     * @param status  estado al que cambia (debe ser el valor coincidente con el enum)
     * @return pedido actualizado
     * @throws ResponseStatusException si el estado o la transición no son válidos
     */
    @Transactional
    public OrderDTO updateOrder(long idOrder, String status) throws ResponseStatusException {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido inválido");
        }
        SaleStatus newStatus;
        try {
            newStatus = SaleStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pedido inválido");
        }

        Order order = getOrderByIdForUpdate(idOrder);
        SaleStatus currentStatus = order.getStatus();
        if (currentStatus == newStatus) {
            return orderMapper.orderToOrderDTO(order);
        }
        if (currentStatus == null || !currentStatus.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transición de estado no permitida.");
        }

        List<Map.Entry<String, Integer>> itemsToRestore =
                newStatus == SaleStatus.CANCELLED ? extractRestorableLines(order) : List.of();

        int updated = orderRepository.transitionStatus(
                idOrder, currentStatus, newStatus, LocalDateTime.now(Clock.systemUTC()));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estado del pedido ha cambiado concurrentemente.");
        }

        if (newStatus == SaleStatus.CANCELLED) {
            restoreStockForLines(itemsToRestore);
        }

        return orderMapper.orderToOrderDTO(getOrderById(idOrder));
    }

    private List<Map.Entry<String, Integer>> extractRestorableLines(Order order) {
        if (order.getOrderDetails() == null) {
            return List.of();
        }
        return order.getOrderDetails().stream()
                .filter(od -> od.getProduct() != null
                        && od.getProduct().getReference() != null
                        && od.getAmount() != null
                        && od.getAmount() > 0)
                .map(od -> Map.entry(od.getProduct().getReference(), od.getAmount()))
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }

    private void restoreStockForLines(List<Map.Entry<String, Integer>> lines) {
        for (Map.Entry<String, Integer> line : lines) {
            productRepository.incrementStock(line.getKey(), line.getValue());
        }
    }

    /**
     * Obtiene una lista de los productos que aparezcan en sus pedidos
     *
     * @param user usuario del que obtener
     * @return listado de productos comprados
     */
    @Transactional (readOnly = true)
    public Set<Product> getProductsFromUserOrders(User user) {

        return orderRepository.findByUser(user).stream()
                .flatMap(o -> o.getOrderDetails().stream()
                        .map(OrderDetails::getProduct))
                .collect(Collectors.toSet());
    }
}
