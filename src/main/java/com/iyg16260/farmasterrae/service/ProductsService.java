package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.mapper.products.ProductMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductsService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductMapper productMapper;

    private final int PAGE_SIZE = 20;

    public Page<ProductDTO> getProductList(int page) {
        return productRepository
                .findAll(Pageable.ofSize(PAGE_SIZE).withPage(page))
                .map(p -> productMapper.productToProductDTO(p));
    }

    public ProductDTO getProductDTOByReference(String reference) {
        return productMapper.productToProductDTO(
                productRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"))
        );
    }

    public Product getProductByReference(String reference) throws ResponseStatusException {
        return productRepository.findByReference(reference)
                .orElseThrow(() ->  new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Product not found with referece: "+reference));
    }

}
