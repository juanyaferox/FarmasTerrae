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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;

/**
 * Listados del panel de administración. Las operaciones sobre cada entidad están en
 * {@link com.iyg16260.farmasterrae.controller.admin}
 */
@SecurityRequirement (name = SESSION_COOKIE)
@ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
@ApiResponse (responseCode = "403", ref = FORBIDDEN)
@RestController
@RequestMapping ("/api/admin")
public class AdminController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    @Tag (name = TAG_ADMIN_ORDERS)
    @Operation (summary = "Listar pedidos",
            description = "Listado paginado de todos los pedidos. La búsqueda filtra por nombre, dirección, " +
                    "estado o método de pago")
    @ApiResponse (responseCode = "200", description = "Página de pedidos")
    @GetMapping ("/orders")
    public PagedModel<OrderDTO> getOrders(@Parameter (description = "Número de página, empieza en 0")
                                          @RequestParam (defaultValue = "0", required = false) int page,
                                          @Parameter (description = "Campo por el que ordenar", example = "createdAt")
                                          @RequestParam (required = false) String sort,
                                          @Parameter (description = "Dirección de la ordenación: asc o desc")
                                          @RequestParam (defaultValue = "asc") String dir,
                                          @Parameter (description = "Texto a buscar")
                                          @RequestParam (required = false) String search) {
        Page<OrderDTO> orders = orderService.getAllOrders(page, search, sort, dir);
        return new PagedModel<>(orders);
    }

    @Tag (name = TAG_ADMIN_PRODUCTS)
    @Operation (summary = "Listar productos", description = "Listado paginado de todos los productos")
    @ApiResponse (responseCode = "200", description = "Página de productos")
    @GetMapping ("/products")
    public PagedModel<ProductDTO> getProducts(@Parameter (description = "Número de página, empieza en 0")
                                              @RequestParam (defaultValue = "0", required = false) int page,
                                              @Parameter (description = "Campo por el que ordenar", example = "price")
                                              @RequestParam (required = false) String sort,
                                              @Parameter (description = "Dirección de la ordenación: asc o desc")
                                              @RequestParam (defaultValue = "asc") String dir,
                                              @Parameter (description = "Texto a buscar")
                                              @RequestParam (required = false) String search) {
        Page<ProductDTO> products = productsService.getProductList(page, search, sort, dir);
        return new PagedModel<>(products);
    }

    @Tag (name = TAG_ADMIN_USERS)
    @Operation (summary = "Listar usuarios", description = "Listado paginado de todos los usuarios")
    @ApiResponse (responseCode = "200", description = "Página de usuarios")
    @GetMapping ("/users")
    public PagedModel<UserDTO> getUsers(@Parameter (description = "Número de página, empieza en 0")
                                        @RequestParam (defaultValue = "0", required = false) int page,
                                        @Parameter (description = "Campo por el que ordenar", example = "username")
                                        @RequestParam (required = false) String sort,
                                        @Parameter (description = "Dirección de la ordenación: asc o desc")
                                        @RequestParam (defaultValue = "asc") String dir,
                                        @Parameter (description = "Texto a buscar")
                                        @RequestParam (required = false) String search) {
        Page<UserDTO> users = userService.getAllUsers(page, search, sort, dir);
        return new PagedModel<>(users);
    }

    /**
     * Estados de venta disponibles para los pedidos
     */
    @Tag (name = TAG_ADMIN_ORDERS)
    @Operation (summary = "Listar estados de venta",
            description = "Estados posibles de un pedido con su código y su nombre legible")
    @ApiResponse (responseCode = "200", description = "Listado de estados")
    @GetMapping ("/sale-statuses")
    public List<EnumOptionDTO> getSaleStatuses() {
        return Arrays.stream(SaleStatus.values())
                .map(status -> new EnumOptionDTO(status.name(), status.getValue()))
                .toList();
    }

    /**
     * Perfiles disponibles para los usuarios
     */
    @Tag (name = TAG_ADMIN_USERS)
    @Operation (summary = "Listar perfiles", description = "Perfiles (tipos de usuario) disponibles")
    @ApiResponse (responseCode = "200", description = "Listado de perfiles")
    @GetMapping ("/profiles")
    public List<ProfileDTO> getProfiles() {
        return userService.getProfiles().stream()
                .map(ProfileDTO::from)
                .toList();
    }
}
