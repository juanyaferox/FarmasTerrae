package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductsService {

    @Autowired
    ProductRepository productRepository;

    private final int PAGE_SIZE = 20;

    public Page<Product> getProductList(int page) {
        return productRepository
                .findAll(Pageable.ofSize(PAGE_SIZE).withPage(page));
    }

    public Product getProductById(long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    public Product getProductByReference(String reference) {
        return productRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }
}
