package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping ("/admin")
public class AdminController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    @GetMapping ({"/dashboard/{section}", "/dashboard"})
    public ModelAndView getDashboard(@PathVariable (required = false) String section,
                                     @AuthenticationPrincipal User user,
                                     @RequestParam (defaultValue = "0", required = false) int page) {

        ModelAndView model = new ModelAndView("admin/dashboard")
                .addObject("section", section);

        return processSection(section, model, user, page);
    }

    /**
     * Método_ para añadir secciones sin modificar el GetMapping, manteniendo el principio SOLID
     *
     * @param section seccion del pathvariable
     * @param model   model
     * @param user    user
     * @param page    page
     * @return vista de la seccion deseada
     */
    private ModelAndView processSection(String section, ModelAndView model,
                                        User user, int page) {
        if (section == null)
            return model;

        switch (section) {
            case "orders" -> {
                Page<OrderDTO> orders = orderService.getAllOrders(page);
                model.addObject("orders", orders);
                model.addObject("saleStatuses", SaleEnum.SaleStatus.values());
            }
            case "products" -> {
                Page<ProductDTO> products = productsService.getProductList(page);
                model.addObject("products", products);
            }
            case "users" -> {
                Page<UserDTO> users = userService.getAllUsers(page);
                model.addObject("users", users);
            }
        }
        return model;
    }

    @PutMapping ("/dashboard/orders/update")
    public String updateOrder(@RequestParam long idOrder, @RequestParam String status, RedirectAttributes ra) {
        return handleFlash(() -> orderService.updateOrder(idOrder, status),
                ra,
                "Pedido actualizado con éxito.",
                "orders");
    }

    @DeleteMapping ("/dashboard/orders/delete")
    public String deleteOrder(@RequestParam long idOrder, RedirectAttributes ra) {
        return handleFlash(() -> orderService.deleteOrderById(idOrder),
                ra,
                "Pedido eliminado con éxito.",
                "orders");
    }

    @PostMapping ("/dashboard/products/add")
    public String addProduct(@ModelAttribute ProductDTO productDTO, RedirectAttributes ra) {
//        return handleFlash(() -> productsService.saveProduct(productDTO),
//                ra,
//                "Producto creado con éxito.",
//                "/dashboard/products");
        productsService.saveProduct(productDTO);
        ra.addFlashAttribute("successMessage", "Producto añadido con éxito.");
        return "redirect:/admin/dashboard/products";
    }

    @PutMapping ("/dashboard/products/update")
    public String updateProduct(@RequestParam String reference, RedirectAttributes ra) {
        return handleFlash(() -> productsService.updateProduct(reference),
                ra,
                "Producto actualizado con éxito.",
                "products");
    }

    @DeleteMapping ("/dashboard/products/delete")
    public String deleteProduct(@RequestParam String reference, RedirectAttributes ra) {
        return handleFlash(() -> productsService.deleteProduct(reference),
                ra,
                "Producto eliminado con éxito.",
                "products");
        /*
        <div th:if="${successMessage}" class="bg-green-100 text-green-800 p-2 rounded">
            <span th:text="${successMessage}"></span>
        </div>

        <div th:if="${errorMessage}" class="bg-red-100 text-red-800 p-2 rounded">
            <span th:text="${errorMessage}"></span>
        </div>
         */
    }

    private String handleFlash(Runnable action,
                               RedirectAttributes ra,
                               String successMsg,
                               String redirectPath) {
        try {
            action.run();
            ra.addFlashAttribute("successMessage", successMsg);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard/" + redirectPath;
    }
}
