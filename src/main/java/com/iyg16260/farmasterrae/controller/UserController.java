package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    OrderService orderService;

    @GetMapping("/dashboard")
    public ModelAndView getInfoUser(@AuthenticationPrincipal User user) {
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return new ModelAndView("user/dashboard").addObject("user", userDTO);
    }

    @PostMapping("/dashboard")
    public String setInfoUser(@AuthenticationPrincipal User user, @ModelAttribute UserDTO userdto) {
        try {
            userService.updateUser(user.getId(), userdto);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
        return "redirect:/user/dashboard";
    }

    @GetMapping("/orders")
    public ModelAndView getOrderList(
            @AuthenticationPrincipal User user,
            @RequestParam (defaultValue = "0") int page) {
        return new ModelAndView("user/order-list")
                .addObject("orders", orderService.getOrders(user, page));
    }

    @GetMapping("/order")
    public ModelAndView getOrder(
            @AuthenticationPrincipal User user,
            @RequestParam Long orderId) {
        return new ModelAndView("user/order-details")
                .addObject("order", orderService.getOrder(user, orderId));
    }


}
