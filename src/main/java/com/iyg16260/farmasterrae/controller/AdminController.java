package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.response.EnumOptionDTO;
import com.iyg16260.farmasterrae.dto.user.ProfileDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Listados del panel de administración. Las operaciones sobre cada entidad están en
 * {@link com.iyg16260.farmasterrae.controller.admin}
 */
@RestController
@RequestMapping ("/api/admin")
public class AdminController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    @GetMapping ("/orders")
    public PagedModel<OrderDTO> getOrders(@RequestParam (defaultValue = "0", required = false) int page,
                                          @RequestParam (required = false) String sort,
                                          @RequestParam (defaultValue = "asc") String dir,
                                          @RequestParam (required = false) String search) {
        Page<OrderDTO> orders = orderService.getAllOrders(page, search, sort, dir);
        return new PagedModel<>(orders);
    }

    @GetMapping ("/products")
    public PagedModel<ProductDTO> getProducts(@RequestParam (defaultValue = "0", required = false) int page,
                                              @RequestParam (required = false) String sort,
                                              @RequestParam (defaultValue = "asc") String dir,
                                              @RequestParam (required = false) String search) {
        Page<ProductDTO> products = productsService.getProductList(page, search, sort, dir);
        return new PagedModel<>(products);
    }

    @GetMapping ("/users")
    public PagedModel<UserDTO> getUsers(@RequestParam (defaultValue = "0", required = false) int page,
                                        @RequestParam (required = false) String sort,
                                        @RequestParam (defaultValue = "asc") String dir,
                                        @RequestParam (required = false) String search) {
        Page<UserDTO> users = userService.getAllUsers(page, search, sort, dir);
        return new PagedModel<>(users);
    }

    /**
     * Estados de venta disponibles para los pedidos
     */
    @GetMapping ("/sale-statuses")
    public List<EnumOptionDTO> getSaleStatuses() {
        return Arrays.stream(SaleStatus.values())
                .map(status -> new EnumOptionDTO(status.name(), status.getValue()))
                .toList();
    }

    /**
     * Perfiles disponibles para los usuarios
     */
    @GetMapping ("/profiles")
    public List<ProfileDTO> getProfiles() {
        return userService.getProfiles().stream()
                .map(ProfileDTO::from)
                .toList();
    }
}
