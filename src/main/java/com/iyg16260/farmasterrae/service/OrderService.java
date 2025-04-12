package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.json.PaymentDetails;
import com.iyg16260.farmasterrae.model.*;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import com.iyg16260.farmasterrae.utils.SessionCart;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
     * @param user
     * @param idOrder
     * @return pedido coincidiente con el id, si es del usuario actual
     * @throws ResponseStatusException NOTFOUND o FORBIDDEN
     */
    public Order getOrder(User user, long idOrder) throws ResponseStatusException{

        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        user = userService.getUser(user.getId());

        if (user.getOrderList().stream().anyMatch(o -> !Objects.equals(o.getId(), order.getId())))
            throw new ResponseStatusException
                    (HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");

        return order;
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
    public Order setOrder(User user, SessionCart cart,
                          SaleEnum.SaleStatus saleStatus, SaleEnum.PaymentMethod paymentMethod) {
        var products = cart.getProducts();

        if (products == null || products.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");

        Order order = new Order();
        order.setUser(user);
        order.setStatus(saleStatus);
        order.setPaymentDetails(
                new PaymentDetails(paymentMethod, LocalDateTime.now(Clock.systemUTC()))
        );
        order.setOrderDetails(setOrderDetails(products, order));

        return orderRepository.save(order);
    }


    /**
     * Función para complementar el pedido
     * @param cartProducts
     * @param order
     * @return
     */
    private List<OrderDetails> setOrderDetails (Map<String, Integer> cartProducts, Order order) {

        List<OrderDetails> detailsList = new ArrayList<>();

        cartProducts.forEach((reference, quantity) -> {
            Product product = productsService.getProductByReference(reference);
            if (product == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with referece: "+reference);
            OrderDetails details = new OrderDetails();
            details.setProduct(product);
            details.setOrder(order);
            details.setAmount(quantity);
            detailsList.add(details);
        });

        return detailsList;
    }

    @Transactional
    public OrderDetailsDTO getOrderDetailsDTO(Order order2) {
        Order order = orderRepository.findById(order2.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        // Forzamos la carga de la colección dentro de la transacción
        order.getOrderDetails().size();

        OrderDetailsDTO orderDTO = new OrderDetailsDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .localizedBy(Locale.getDefault());
        orderDTO.setCreatedAt(order.getCreatedAt().format(formatter));
        orderDTO.setUpdatedAt(order.getUpdatedAt().format(formatter));
        orderDTO.setPaymentMethod(order.getPaymentDetails() != null
                ? order.getPaymentDetails().getMethod()
                : null);
        orderDTO.setStatus(order.getStatus() != null ? order.getStatus().getValue() : null);
        orderDTO.setTotalPrice(order.getTotalPrice());

        Map<ProductDTO, Integer> productDTOIntegerMap = new HashMap<>();
        order.getOrderDetails().forEach(o -> {
            var productDTO = new ProductDTO();
            BeanUtils.copyProperties(o.getProduct(), productDTO);
            productDTOIntegerMap.put(productDTO, o.getAmount());
        });
        orderDTO.setProducts(productDTOIntegerMap);

        return orderDTO;
    }
}
