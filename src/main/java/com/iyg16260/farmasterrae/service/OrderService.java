package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.json.PaymentDetails;
import com.iyg16260.farmasterrae.mapper.user.OrderMapper;
import com.iyg16260.farmasterrae.model.*;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
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
     * @return paginas con los pedidos, 50 para admin 10 para usuarios
     */
    public Page<Order> getOrders(User user, int page) {
        int pageSize = "ADMIN".equals(user.getProfile().getType()) ? PAGE_SIZE_ADMIN : PAGE_SIZE_USER;
            Pageable pageable = Pageable
                    .ofSize(pageSize)
                    .withPage(page);
            return orderRepository.findByUser(user, pageable);
    }

    /**
     * @param idUser
     * @param idOrder
     * @return pedido coincidiente con el id, si es del usuario actual
     * @throws ResponseStatusException NOTFOUND o FORBIDDEN
     */
    @Transactional
    public OrderDetailsDTO getOrder(long idUser, long idOrder) throws ResponseStatusException{

        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        User user = userService.getUser(idUser);

        if (user.getOrderList().stream().anyMatch(o -> !Objects.equals(o.getId(), order.getId())))
            throw new ResponseStatusException
                    (HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");

        return orderMapper.orderToOrderDetailsDTO(order);
    }

    /**
     * Obtiene una lista de productos a partir de un pedido
     * @param idOrder
     * @return
     */
    public List<Product> getProductsFromOrder(long idOrder) {

        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return order.getOrderDetails().stream()
                .flatMap(o -> IntStream.range(0, o.getAmount())
                        .mapToObj(i -> o.getProduct()))
                .toList();
    }

    /**
     * Guarda un pedido
     * @param user
     * @param cart
     * @param saleStatus
     * @param paymentMethod
     * @return
     */
    public OrderDetailsDTO setOrder(User user, SessionCart cart,
                                    SaleEnum.SaleStatus saleStatus, SaleEnum.PaymentMethod paymentMethod)
            throws ResponseStatusException {
        var products = cart.getProducts();

        if (products == null || products.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        Order order = new Order();
        order.setUser(user);
        order.setStatus(saleStatus);
        order.setPaymentDetails(
                new PaymentDetails(paymentMethod, LocalDateTime.now(Clock.systemUTC()))
        );
        order.setOrderDetails(
                getOrderDetailsFromCart(products, order)
        );

        return orderMapper.orderToOrderDetailsDTO(orderRepository.save(order));
    }


    /**
     * Función para complementar el pedido
     * @param cartProducts
     * @param order
     * @return
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
}
