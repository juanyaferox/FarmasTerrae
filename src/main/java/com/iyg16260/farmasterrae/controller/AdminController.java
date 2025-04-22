package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import org.h2.engine.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
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
     * @param section seccion del pathvariable
     * @param model model
     * @param user user
     * @param page page
     * @return vista de la seccion deseada
     */
    private ModelAndView processSection (String section, ModelAndView model,
                                        User user, int page) {
        if (section == null)
            return model;

        switch (section) {
            case "orders" -> {
                Page<OrderDTO> orders = orderService.getOrders(user, page);
                model.addObject("orders", orders);
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
    public String updateOrder (@PathVariable long idOrder, RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrder(idOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido actualizado con éxito.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hubo un error al actualizar el pedido.");
        }
        return "redirect:/dashboard/orders";
    }

    @DeleteMapping ("/dashboard/orders/delete")
    public String deleteOrder (@PathVariable long idOrder, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrderById(idOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido eliminado con éxito.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hubo un error al eliminar el pedido.");
        }
        return "redirect:/dashboard/orders";
    }

    @PostMapping ("/dashboard/products/add")
    public String addProduct (@ModelAttribute ProductDTO productDTO, RedirectAttributes redirectAttributes) {
        try {
            productsService.saveProduct(productDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido creado con éxito.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hubo un error al crear el producto.");
        }
        return "redirect:/dashboard/products";
    }

    @PutMapping ("/dashboard/products/update")
    public String updateProduct (@PathVariable String reference, RedirectAttributes redirectAttributes) {
        try {
            productsService.updateProduct(reference);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido actualizado con éxito.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hubo un error al actualizar el pedido.");
        }
        return "redirect:/dashboard/products";
    }

    @DeleteMapping("/dashboard/products/delete")
    public String deleteProduct (@PathVariable String reference, RedirectAttributes redirectAttributes) {
        try {
            productsService.deleteProduct(reference);
            redirectAttributes.addFlashAttribute("successMessage", "Producto eliminado con éxito.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hubo un error al eliminar el producto.");
        }
        return "redirect:/dashboard/products";
        /*
        <div th:if="${successMessage}" class="bg-green-100 text-green-800 p-2 rounded">
            <span th:text="${successMessage}"></span>
        </div>

        <div th:if="${errorMessage}" class="bg-red-100 text-red-800 p-2 rounded">
            <span th:text="${errorMessage}"></span>
        </div>
         */
    }
}
