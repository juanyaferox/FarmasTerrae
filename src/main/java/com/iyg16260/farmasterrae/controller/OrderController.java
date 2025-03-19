package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.AuthUserService;
import com.iyg16260.farmasterrae.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@Slf4j
@RequestMapping ("/order")
public class OrderController {
    @Autowired
    OrderService os;

    @Autowired
    AuthUserService auth;

    @GetMapping
    public String getOrder(@RequestParam (defaultValue = "0") int page){
        var orderList = os.getOrder(auth.getAuthUser(), Pageable.ofSize(10).withPage(page));
        return "";
    }
}
