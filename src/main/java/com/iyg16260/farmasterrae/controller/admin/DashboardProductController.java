package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.ProductsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

/**
 * Los productos se envían como multipart/form-data para poder adjuntar la imagen (campo imageFile)
 */
@RestController
@RequestMapping ("/api/admin/products")
public class DashboardProductController {

    @Autowired
    ProductsService productsService;

    @PostMapping (consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus (HttpStatus.CREATED)
    public MessageResponseDTO addProduct(@Valid @ModelAttribute ProductDTO productDTO,
                                         @RequestParam (required = false) MultipartFile imageFile) {
        productsService.saveProduct(productDTO);
        return buildSuccessMessage(EntityType.PRODUCTS, Operation.POST);
    }

    @PutMapping (value = "/{oldReference}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponseDTO updateProduct(@Valid @ModelAttribute ProductDTO productDTO,
                                            @PathVariable String oldReference,
                                            @RequestParam (required = false) MultipartFile imageFile) {
        productsService.updateProduct(productDTO, oldReference, imageFile);
        return buildSuccessMessage(EntityType.PRODUCTS, Operation.PUT);
    }

    @DeleteMapping ("/{reference}")
    public MessageResponseDTO deleteProduct(@PathVariable String reference) {
        productsService.deleteProduct(reference);
        return buildSuccessMessage(EntityType.PRODUCTS, Operation.DELETE);
    }
}
