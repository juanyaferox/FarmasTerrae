package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

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
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Product not found with referece: " + reference));
    }

    public void deleteProduct(String reference) throws ResponseStatusException {
        System.out.println("++++++ Reference: " + reference);
        Product product = getProductByReference(reference);
        productRepository.delete(product);
    }

    public ProductDTO updateProduct(ProductDTO productDTO, String oldReference) throws ResponseStatusException {
        Product product = productMapper.productDTOToProduct(productDTO);

        if (!Objects.equals(product.getReference(), oldReference) && productRepository.existsByReference(product.getReference()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already exists a product with that reference");

        product.setId(productRepository.findByReference(oldReference).orElseThrow(() -> new ResponseStatusException
                (HttpStatus.NOT_FOUND, "Product not found with referece: " + oldReference)).getId());

        return productMapper.productToProductDTO(
                productRepository.save(product)
        );
    }

    public Product saveProduct(ProductDTO productDTO) {
        Product product = productMapper.productDTOToProduct(productDTO);

        if (productRepository.existsByReference(product.getReference())) {
            throw new ResponseStatusException
                    (HttpStatus.CONFLICT, "Product already exists by: " + product.getReference());
        }

        return productRepository.save(product);
    }

}
