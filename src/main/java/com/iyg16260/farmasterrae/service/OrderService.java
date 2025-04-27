package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.mapper.user.OrderMapper;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.OrderDetails;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

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

    // No utilizar repositorio de OrderDetails, no es necesario.

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

        if (user.getOrderList().stream().anyMatch(o -> !Objects.equals(o.getId(), order.getId())))
            throw new ResponseStatusException
                    (HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");

        return orderMapper.orderToOrderDetailsDTO(order);
    }

    private Order getOrderById(long idOrder) throws ResponseStatusException {
        return orderRepository.findById(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
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
     * @param user          usuario
     * @param cart          carrito
     * @param saleStatus    saleStatus
     * @param paymentMethod paymentMethod
     * @return detalles del pedido guardado
     */
    public OrderDetailsDTO setOrder(User user, SessionCart cart,
                                    SaleEnum.SaleStatus saleStatus,
                                    SaleEnum.PaymentMethod paymentMethod) throws ResponseStatusException {
        var products = cart.getProducts();

        if (products == null || products.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        Order order = new Order();
        order.setUser(user);
        order.setStatus(saleStatus);
        order.setPaymentMethod(paymentMethod);
        order.setOrderDetails(
                getOrderDetailsFromCart(products, order)
        );

        return orderMapper.orderToOrderDetailsDTO(orderRepository.save(order));
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
            OrderDetails details = new OrderDetails();
            details.setProduct(product);
            details.setOrder(order);
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
            order.setStatus(SaleEnum.SaleStatus.valueOf(status.toUpperCase()));
            return orderMapper.orderToOrderDTO(
                    orderRepository.save(order)
            );
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + status);
        }
    }
}
