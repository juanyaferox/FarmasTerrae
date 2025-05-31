package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ReviewService;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@Slf4j
@Controller
@RequestMapping ("/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    OrderService orderService;
    
    @Autowired
    ReviewService reviewService;

    /**
     * Para acceder al dashboard y cambiar entre secciones
     *
     * @param section nombre de la seccion a validar en el if ternario de la vista, no obligatorio
     * @param page    pagina actual de la seccion de pedido, no obligatorio
     * @return dashboard + seccion solicitada, caso no coincida solo el dashboard
     */
    @GetMapping ({"/dashboard/{section}", "/dashboard"})
    public ModelAndView changeSection(@PathVariable (required = false) String section,
                                      @RequestParam (defaultValue = "0", required = false) int page,
                                      @RequestParam (required = false) String sort,
                                      @RequestParam (defaultValue = "asc") String dir) {

        ModelAndView model = new ModelAndView("user/dashboard")
                .addObject("section", section);

        return processSection(section, model, page, sort, dir);
    }

    /**
     * Método_ para añadir secciones sin modificar el GetMapping, manteniendo el principio SOLID
     *
     * @param section seccion del pathvariable
     * @param model   model
     * @param page    page
     * @return vista de la seccion deseada
     */
    private ModelAndView processSection(String section, ModelAndView model, int page, String sort, String dir) {

        User user = null;
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User authUser)
            user = authUser;

        if (section == null)
            return model;

        switch (section) {
            case "orders" -> {
                Page<OrderDTO> orders = orderService.getOrders(user, page, sort, dir);
                return model.addObject("orders", orders);
            }
            case "info-user" -> {
                UserDTO userDTO = userService.getUserById(user.getId());
                return model.addObject("userView", userDTO);
            }
            case "reviews" -> {
                Page<ReviewDTO> reviews = reviewService.getReviews(user, page, sort, dir);
                List<ProductDTO> products = reviewService.getProductForReview(user);
                return model.addObject("reviews", reviews)
                        .addObject("products", products);
            }
        }

        return model;
    }

    /**
     * Cambia la informacion del usuario recibiendo el userDto
     *
     * @param user
     * @param userdto
     * @return redirecciona al dashboard
     */
    @PostMapping ("/dashboard/info-user")
    public ModelAndView setInfoUser(@AuthenticationPrincipal User user, @ModelAttribute ("userView") UserDTO userdto, RedirectAttributes ra) {
        userService.updateUserById(user.getId(), userdto);
        buildSuccessMessage(ra, EntityType.USERS, Operation.POST);
        return new ModelAndView("redirect:/user/dashboard/info-user");
    }

    /**
     * Vista de detalles de un pedido con la informacion del pedido y una lista de sus productos
     *
     * @param idOrder
     * @param user
     * @return
     */
    @GetMapping ("/dashboard/orders/{idOrder}")
    public ModelAndView getOrder(@PathVariable int idOrder, @RequestParam (defaultValue = "false", required = false) boolean success, @AuthenticationPrincipal User user) {
        OrderDetailsDTO orderDetails = orderService.getOrder(user.getId(), idOrder);
        System.out.println(orderDetails);
        return new ModelAndView("user/order-details")
                .addObject("order", orderDetails)
                .addObject("success", success);
    }

    @PutMapping ("/dashboard/reviews")
    public String updateReview(@AuthenticationPrincipal User user,
                               @Valid @ModelAttribute ReviewDTO reviewDTO, RedirectAttributes ra) {
        reviewService.updateReview(reviewDTO, user);
        buildSuccessMessage(ra, EntityType.REVIEWS, Operation.PUT);
        return "redirect:/user/dashboard/reviews";
    }

    @PostMapping ("/dashboard/reviews")
    public String addReview(@AuthenticationPrincipal User user,
                            @Valid @ModelAttribute ReviewDTO reviewDTO, RedirectAttributes ra) {
        reviewService.setReview(reviewDTO, user);
        buildSuccessMessage(ra, EntityType.REVIEWS, Operation.POST);
        return "redirect:/user/dashboard/reviews";
    }

    @DeleteMapping ("/dashboard/reviews")
    public String deleteReview(@RequestParam long idReview,
                               @AuthenticationPrincipal User user, RedirectAttributes ra) {
        reviewService.deleteReview(idReview, user);
        buildSuccessMessage(ra, EntityType.REVIEWS, Operation.DELETE);
        return "redirect:/user/dashboard/reviews";

    }
}
