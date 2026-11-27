package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.dto.user.SelfProfileUpdateDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.PrincipalRefreshService;
import com.iyg16260.farmasterrae.service.ReviewService;
import com.iyg16260.farmasterrae.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@Controller
@RequestMapping ("/user")
public class UserController {

    /** Secciones que existen en {@code user/dashboard.html} */
    private static final Set<String> SECTIONS = Set.of("info-user", "orders", "reviews");

    @Autowired
    UserService userService;

    @Autowired
    OrderService orderService;

    @Autowired
    ReviewService reviewService;

    @Autowired
    PrincipalRefreshService principalRefreshService;

    /**
     * Para acceder al dashboard y cambiar entre secciones
     *
     * @param section nombre de la seccion a validar en el if ternario de la vista, no obligatorio
     * @param page    pagina actual de la seccion de pedido, no obligatorio
     * @return dashboard + seccion solicitada, o solo el dashboard si no se indica ninguna
     * @throws ResponseStatusException NOT_FOUND si la sección no existe
     */
    @GetMapping ({"/dashboard/{section}", "/dashboard"})
    public ModelAndView changeSection(@PathVariable (required = false) String section,
                                      @RequestParam (defaultValue = "0", required = false) int page,
                                      @RequestParam (required = false) String sort,
                                      @RequestParam (defaultValue = "asc") String dir) {
        if (section != null && !SECTIONS.contains(section)) {
            ModelAndView notFound = new ModelAndView("common/error");
            notFound.setStatus(HttpStatus.NOT_FOUND);
            notFound.addObject("errorCode", HttpStatus.NOT_FOUND.value());
            notFound.addObject("errorMessageLabel", "Sección no encontrada");
            return notFound;
        }

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
                SelfProfileUpdateDTO userView = userService.getOwnProfile(user.getId());
                return model.addObject("userView", userView);
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
     * Cambia la información del propio usuario. El formulario solo puede cambiar los campos de
     * {@link SelfProfileUpdateDTO}: cualquier otro parámetro (p. ej. {@code type}) se ignora
     *
     * @param user           usuario autenticado
     * @param selfProfileDTO nuevos datos del usuario
     * @param result         errores de validación
     * @return redirecciona al formulario; si hay errores, lo vuelve a mostrar con ellos (400)
     */
    @PostMapping ("/dashboard/info-user")
    public ModelAndView setInfoUser(@AuthenticationPrincipal User user,
                                    @Valid @ModelAttribute ("userView") SelfProfileUpdateDTO selfProfileDTO,
                                    BindingResult result, RedirectAttributes ra,
                                    HttpServletRequest request, HttpServletResponse response) {
        if (result.hasErrors()) {
            return new ModelAndView("user/dashboard", HttpStatus.BAD_REQUEST)
                    .addObject("section", "info-user")
                    .addObject("errorMessage", "Revisa los campos indicados.");
        }

        HttpSession session = request.getSession(false);
        User updated = userService.updateOwnProfile(user.getId(), selfProfileDTO, session == null ? null : session.getId());
        principalRefreshService.refresh(updated, request, response);
        buildSuccessMessage(ra, EntityType.USERS, Operation.PUT);
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
    public ModelAndView getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user) {
        OrderDetailsDTO orderDetails = orderService.getOrder(user.getId(), idOrder);
        return new ModelAndView("user/order-details")
                .addObject("order", orderDetails);
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
