package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.payment.PaymentDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.mapper.OrderMapper;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.iyg16260.farmasterrae.utils.GenericUtils.priceAmountCalc;

@Service
public class OrderService {

    private final int PAGE_SIZE_USER = 10;
    private final int PAGE_SIZE_ADMIN = 50;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    CartService cartService;

    /**
     * @param user usuario a obtener pedidos
     * @return paginas con los pedidos del user, si eres admin de todos
     */
    public Page<OrderDTO> getOrders(User user, int page) {
        Pageable pageable = Pageable
                .ofSize(PAGE_SIZE_USER)
                .withPage(page);

        return orderRepository.findByUser(user, pageable)
                .map(orderMapper::orderToOrderDTO);
    }

    public List<Order> getAllOrderFromUser(User user) {
        return orderRepository.findByUser(user);
    }

    public Page<OrderDTO> getAllOrders(int page) {
        Pageable pageable = Pageable
                .ofSize(PAGE_SIZE_ADMIN)
                .withPage(page);

        return orderRepository.findAll(pageable)
                .map(orderMapper::orderToOrderDTO);
    }

    /**
     * @param idUser  idUser
     * @param idOrder idOrder
     * @return pedido coincidiente con el id, si es del usuario actual
     * @throws ResponseStatusException NOTFOUND o FORBIDDEN
     */
    @Transactional
    public OrderDetailsDTO getOrder(long idUser, long idOrder) throws ResponseStatusException {

        Order order = getOrderById(idOrder);

        User user = userService.getUser(idUser);

        if (Objects.equals(user.getProfile().getType(), "ADMIN"))
            return orderMapper.orderToOrderDetailsDTO(order);

        if (user.getOrderList().stream().anyMatch(o -> !Objects.equals(o.getId(), order.getId())))
            throw new ResponseStatusException
                    (HttpStatus.FORBIDDEN, "No tienes permiso para acceder a este recurso.");

        return orderMapper.orderToOrderDetailsDTO(order);
    }

    private Order getOrderById(long idOrder) throws ResponseStatusException {
        return orderRepository.findById(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    /**
     * Obtiene una lista de productos a partir de un pedido
     *
     * @param idOrder idOrder
     * @return lista de productos de un pedido
     */
    public List<Product> getProductsFromOrder(long idOrder) {

        Order order = getOrderById(idOrder);

        return order.getOrderDetails().stream()
                .flatMap(o -> IntStream.range(0, o.getAmount())
                        .mapToObj(i -> o.getProduct()))
                .toList();
    }

    /**
     * Guarda un pedido
     *
     * @param user       usuario
     * @param cart       carrito
     * @param saleStatus saleStatus
     * @param payment    paymentMethod
     * @param session    la sesión HTTP
     * @return detalles del pedido guardado
     */
    @Transactional
    public Order setOrder(User user, SessionCart cart,
                          SaleStatus saleStatus,
                          PaymentDetailsDTO payment,
                          HttpSession session) throws ResponseStatusException {
        var products = cart.getProducts();

        if (products == null || products.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrito no encontrado");

        // Verificar que las reservas de stock son válidas
        if (!cartService.validateCartReservations(session)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay suficiente stock disponible o la reserva ha expirado. Por favor, revisa tu carrito.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(saleStatus);
        order.setTotalPrice(priceAmountCalc(cartService.getDetailedProducts(session)));
        order.setPaymentMethod(payment.getPaymentMethod());
        order.setAddress(payment.getAddress());
        order.setName(payment.getFull_name());
        Order savedOrder = orderRepository.save(order);
        order.setOrderDetails(
                getOrderDetailsFromCart(products, savedOrder)
        );


        // Confirmar la reserva de stock (actualiza el stock real)
        if (!cartService.confirmReservation(session)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al confirmar la reserva de stock");
        }

        return savedOrder;
    }


    /**
     * Función para complementar el pedido
     *
     * @param cartProducts productos del carrito
     * @param order        pedido
     * @return lista de detalles del pedido para guardar en el pedido
     */
    private List<OrderDetails> getOrderDetailsFromCart(Map<String, Integer> cartProducts, Order order)
            throws ResponseStatusException {

        List<OrderDetails> detailsList = new ArrayList<>();

        cartProducts.forEach((reference, quantity) -> {
            Product product = productsService.getProductByReference(reference);
            OrderDetails details = new OrderDetails(order, product);
            details.setAmount(quantity);
            detailsList.add(details);
        });

        return detailsList;
    }

    public void deleteOrderById(long idOrder) throws ResponseStatusException {
        Order order = getOrderById(idOrder);
        orderRepository.delete(order);
    }

    @Transactional
    public OrderDTO updateOrder(long idOrder, String status) throws ResponseStatusException {
        try {
            System.out.println("idOrder: " + idOrder + ", status: " + status);
            Order order = getOrderById(idOrder);
            order.setStatus(SaleStatus.valueOf(status.toUpperCase()));
            return orderMapper.orderToOrderDTO(
                    orderRepository.save(order)
            );
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + status);
        }
    }

    @Transactional
    public Set<Product> getProductsFromUserOrders(User user) {

        return orderRepository.findByUser(user).stream()
                .flatMap(o -> o.getOrderDetails().stream()
                        .map(OrderDetails::getProduct))
                .collect(Collectors.toSet());
    }
}