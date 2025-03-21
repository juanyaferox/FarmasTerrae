package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final int PAGE_SIZE = 10;

    @Autowired
    OrderRepository orderRepository;


    /**
     * @param user usuario a obtener pedidos
     * @return paginas con los pedidos, 50 para admin 10 para usuarios
     */
    public Page<Order> getOrders(User user, int page) {
        Pageable pageable = Pageable
                .ofSize(PAGE_SIZE)
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

        if (user.getOrderList().stream().anyMatch(a -> a.getId().equals(order.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
        }

        return order;
    }
}
