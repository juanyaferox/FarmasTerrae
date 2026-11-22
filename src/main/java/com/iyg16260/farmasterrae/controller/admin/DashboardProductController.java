package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductFormDTO;
import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.ProductsService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;
import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

/**
 * Los productos se envían como multipart/form-data para poder adjuntar la imagen (campo imageFile)
 */
@Tag (name = TAG_ADMIN_PRODUCTS)
@SecurityRequirement (name = SESSION_COOKIE)
@ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
@ApiResponse (responseCode = "403", ref = FORBIDDEN)
@RestController
@RequestMapping ("/api/admin/products")
public class DashboardProductController {

    @Autowired
    ProductsService productsService;

    @io.swagger.v3.oas.annotations.Operation (summary = "Crear un producto",
            description = "Crea un producto a partir de los campos del formulario (multipart/form-data). " +
                    "Actualmente la imagen solo se procesa al actualizar el producto, no al crearlo")
    @ApiResponse (responseCode = "201", description = "Producto creado")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @io.swagger.v3.oas.annotations.parameters.RequestBody (required = true, content = @Content (
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema (implementation = ProductFormDTO.class)))
    @PostMapping (consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus (HttpStatus.CREATED)
    public MessageResponseDTO addProduct(@Valid @Parameter (hidden = true) @ModelAttribute ProductDTO productDTO,
                                         @Parameter (hidden = true)
                                         @RequestParam (required = false) MultipartFile imageFile) {
        productsService.saveProduct(productDTO);
        return buildSuccessMessage(EntityType.PRODUCTS, Operation.POST);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Actualizar un producto",
            description = "Actualiza el producto con la referencia indicada en la ruta (la referencia puede " +
                    "cambiarse en el formulario). Si se adjunta imageFile, reemplaza la imagen")
    @ApiResponse (responseCode = "200", description = "Producto actualizado")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @io.swagger.v3.oas.annotations.parameters.RequestBody (required = true, content = @Content (
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema (implementation = ProductFormDTO.class)))
    @PutMapping (value = "/{oldReference}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponseDTO updateProduct(@Valid @Parameter (hidden = true) @ModelAttribute ProductDTO productDTO,
                                            @Parameter (description = "Referencia actual del producto",
                                                    example = "REFMED001")
                                            @PathVariable String oldReference,
                                            @Parameter (hidden = true)
                                            @RequestParam (required = false) MultipartFile imageFile) {
        productsService.updateProduct(productDTO, oldReference, imageFile);
        return buildSuccessMessage(EntityType.PRODUCTS, Operation.PUT);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Eliminar un producto")
    @ApiResponse (responseCode = "200", description = "Producto eliminado")
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @DeleteMapping ("/{reference}")
    public MessageResponseDTO deleteProduct(@Parameter (description = "Referencia del producto", example = "REFMED001")
                                            @PathVariable String reference) {
        productsService.deleteProduct(reference);
        return buildSuccessMessage(EntityType.PRODUCTS, Operation.DELETE);
    }
}
