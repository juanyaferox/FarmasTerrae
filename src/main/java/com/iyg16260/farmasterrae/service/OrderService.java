package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    OrderRepository or;

    /**
     * @param user usuario a obtener pedidos
     * @return paginas con los pedidos, 50 para admin 10 para usuarios
     */
    public Page<Order> getOrder(User user, Pageable pageable) {
        if (user==null)
            return null;
        ///  verificar si es admin, caso sea asi, buscar por find by type con admin
        if (user.getProfile().getId()==1L)
            return or.findAll(pageable);
        return or.findByUser(user, pageable);
    }
}
