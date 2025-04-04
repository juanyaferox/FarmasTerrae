package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public ModelAndView showDashboard(@AuthenticationPrincipal User user) {
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        System.out.println(userDTO.toString());
        return new ModelAndView("user/dashboard").addObject("user", userDTO);
    }

    @GetMapping("/dashboard/{section}")
    public ModelAndView changeSection(@PathVariable String section, @AuthenticationPrincipal User user,
                                      @RequestParam(defaultValue = "0", required = false) int pageOrders) {

        ModelAndView model = new ModelAndView("user/dashboard")
                .addObject("section", section);

        System.out.println(section);
        if (section.equals("info-user")) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            return model.addObject("userView", userDTO);
        }
        else if (section.equals("orders")) {
            Page<Order> orders = orderService.getOrders(user, pageOrders);
            return model.addObject("orders", orders);
        }

        return model;
    }

    @PostMapping("/dashboard/info-user")
    public String setInfoUser(@AuthenticationPrincipal User user, @ModelAttribute UserDTO userdto) {
        try {
            userService.updateUser(user.getId(), userdto);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
        return "redirect:/user/dashboard";
    }

    @GetMapping("/order")
    public ModelAndView getOrder(
            @AuthenticationPrincipal User user,
            @RequestParam Long orderId) {
        return new ModelAndView("user/order-details")
                .addObject("order", orderService.getOrder(user, orderId));
    }
}
