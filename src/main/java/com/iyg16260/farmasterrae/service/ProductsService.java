package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ProductsService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ReviewMapper reviewMapper;

    @Autowired
    ProductMapper productMapper;

    private final int PAGE_SIZE = 24;
    private final int PAGE_SIZE_ADMIN = 10;

    /**
     * Obtiene una página de productos con opción de tamaño personalizado para admin
     *
     * @param page    número de página
     * @param isAdmin si es admin para usar tamaño de página diferente
     * @return página de productos DTO
     */
    public Page<ProductDTO> getProductList(int page, boolean isAdmin) {
        int pageSize = isAdmin ? PAGE_SIZE_ADMIN : PAGE_SIZE;
        return productRepository
                .findAll(Pageable.ofSize(pageSize).withPage(page))
                .map(p -> productMapper.productToProductDTO(p));
    }

    /**
     * Obtiene una página de productos con tamaño estándar
     *
     * @param page número de página
     * @return página de productos DTO
     */
    public Page<ProductDTO> getProductList(int page) {
        return getProductList(page, false);
    }

    /**
     * Obtiene una página de productos filtrados por categoría
     *
     * @param category categoría a filtrar
     * @param page     número de página
     * @return página de productos DTO para página de productos
     */
    public Page<ProductPageDTO> getProductListByCategory(Category category, int page) {
        return productRepository
                .findByCategory(category, Pageable.ofSize(PAGE_SIZE).withPage(page))
                .map(productMapper::productToProductPageDTO);
    }

    /**
     * Obtiene una página de todos los productos sin filtro de categoría
     *
     * @param page número de página
     * @return página de productos DTO para página de productos
     */
    public Page<ProductPageDTO> getProductListByCategory(int page) {
        return productRepository
                .findAll(Pageable.ofSize(PAGE_SIZE).withPage(page))
                .map(productMapper::productToProductPageDTO);
    }

    /**
     * Obtiene un producto DTO por su referencia
     *
     * @param reference referencia del producto
     * @return producto DTO
     */
    public ProductDTO getProductDTOByReference(String reference) {
        return productMapper.productToProductDTO(
                productRepository.findByReference(reference)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"))
        );
    }

    /**
     * Obtiene un producto por su referencia
     *
     * @param reference referencia del producto
     * @return producto encontrado
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el producto
     */
    public Product getProductByReference(String reference) throws ResponseStatusException {
        return productRepository.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Product not found with referece: " + reference));
    }

    /**
     * Elimina un producto por su referencia
     *
     * @param reference referencia del producto a eliminar
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el producto
     */
    public void deleteProduct(String reference) throws ResponseStatusException {
        Product product = getProductByReference(reference);
        productRepository.delete(product);
    }

    /**
     * Actualiza un producto existente
     *
     * @param productDTO   DTO con los datos actualizados del producto
     * @param oldReference referencia anterior del producto
     * @return producto DTO actualizado
     * @throws ResponseStatusException BAD_REQUEST si la nueva referencia ya existe, NOT_FOUND si no se encuentra el producto
     */
    public ProductDTO updateProduct(ProductDTO productDTO, String oldReference) throws ResponseStatusException {
        Product product = productMapper.productDTOToProduct(productDTO);

        if (!Objects.equals(product.getReference(), oldReference) && productRepository.existsByReference(product.getReference()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un producto con esa referencia");

        product.setId(productRepository.findByReference(oldReference).orElseThrow(() -> new ResponseStatusException
                (HttpStatus.NOT_FOUND, "Producto no encontrado con esta referencia: " + oldReference)).getId());

        return productMapper.productToProductDTO(
                productRepository.save(product)
        );
    }

    /**
     * Guarda un nuevo producto en la base de datos
     *
     * @param productDTO DTO del producto a guardar
     * @return producto guardado
     * @throws ResponseStatusException BAD_REQUEST si ya existe un producto con esa referencia
     */
    public Product saveProduct(ProductDTO productDTO) {
        Product product = productMapper.productDTOToProduct(productDTO);
        if (productRepository.existsByReference(product.getReference())) {
            throw new ResponseStatusException
                    (HttpStatus.BAD_REQUEST, "Ya existe un producto con esa referencia: " + product.getReference());
        }

        return productRepository.save(product);
    }

    /**
     * Obtiene todas las reviews de un producto por su referencia
     *
     * @param reference referencia del producto
     * @return lista de reviews DTO del producto
     */
    @Transactional
    public List<ReviewDTO> getReviewsFromProduct(String reference) {
        Product product = getProductByReference(reference);
        return product.getReviewList().stream().map(reviewMapper::reviewToReviewDTO).toList();
    }

}
