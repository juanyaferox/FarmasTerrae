package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.spec.ProductSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ProductsService {

    @Autowired
    S3StorageService s3StorageService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ReviewMapper reviewMapper;

    @Autowired
    ProductMapper productMapper;

    private final int PAGE_SIZE = 12;
    private final int PAGE_SIZE_ADMIN = 10;
    private final String PRODUCT_IMAGES_FOLDER = "product-images";
    private static final Duration DEFAULT_URL_DURATION = Duration.ofHours(2);

    /**
     * Obtiene una página de productos con opción de tamaño personalizado para admin
     *
     * @param page número de página
     * @return página de productos DTO
     */
    public Page<ProductDTO> getProductList(int page) {
        return productRepository
                .findAll(Pageable.ofSize(PAGE_SIZE_ADMIN).withPage(page))
                .map(productMapper::productToProductDTO);
    }

    public Page<ProductPageDTO> getFilteredProducts(Category category, String keyword, int page) {
        Specification<Product> spec = Specification.where(null);
        ProductSpecification specUtil = new ProductSpecification();

        if (category != null) {
            spec = spec.and(specUtil.searchByCategory(category));
        }

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(specUtil.searchInReferenceNameDescription(keyword));
        }

        Page<Product> products = productRepository.findAll(spec, Pageable.ofSize(PAGE_SIZE).withPage(page));
        return products.map(productMapper::productToProductPageDTO);
    }

    /**
     * Obtiene un producto DTO por su referencia
     *
     * @param reference referencia del producto
     * @return producto DTO
     */
    public ProductDTO getProductDTOByReference(String reference) {
        Product product = productRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        return productMapper.productToProductDTO(product);
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
    public ProductDTO updateProduct(ProductDTO productDTO, String oldReference, MultipartFile image) throws ResponseStatusException {
        Product product = productMapper.productDTOToProduct(productDTO);
        Product oldProduct = productRepository.findByReference(oldReference).orElseThrow(() -> new ResponseStatusException
                (HttpStatus.NOT_FOUND, "Producto no encontrado con esta referencia: " + oldReference));

        if (!Objects.equals(product.getReference(), oldReference) && productRepository.existsByReference(product.getReference()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un producto con esa referencia");

        product.setId(oldProduct.getId());

        if (image != null && !image.isEmpty()) {
            // Si ya había una imagen, eliminarla de S3
            if (oldProduct.getImagePath() != null && !oldProduct.getImagePath().isEmpty()) {
                s3StorageService.deleteFile(oldProduct.getImagePath());
            }

            // Subir la nueva imagen
            String imageUrl = s3StorageService.uploadFile(image, PRODUCT_IMAGES_FOLDER);
            product.setImagePath(imageUrl);
        } else {
            // Mantener la imagen anterior
            product.setImagePath(oldProduct.getImagePath());
        }

        return productMapper.productToProductDTO(
                productRepository.save(product)
        );
    }

    public ProductDTO updateProduct(ProductDTO productDTO, String oldReference) throws ResponseStatusException {
        return updateProduct(productDTO, oldReference, null);
    }

    /**
     * Guarda un nuevo producto en la base de datos
     *
     * @param productDTO DTO del producto a guardar
     * @return producto guardado
     * @throws ResponseStatusException BAD_REQUEST si ya existe un producto con esa referencia
     */
    public Product saveProduct(ProductDTO productDTO, MultipartFile image) {
        Product product = productMapper.productDTOToProduct(productDTO);

        if (productRepository.existsByReference(product.getReference())) {
            throw new ResponseStatusException
                    (HttpStatus.BAD_REQUEST, "Ya existe un producto con esa referencia: " + product.getReference());
        }

        if (image != null && !image.isEmpty()) {
            String imageUrl = s3StorageService.uploadFile(image, PRODUCT_IMAGES_FOLDER);
            product.setImagePath(imageUrl);
        }

        return productRepository.save(product);
    }

    public Product saveProduct(ProductDTO productDTO) {
        return saveProduct(productDTO, null);
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

    private ProductDTO convertToProductDTOWithSignedUrl(Product product) {
        ProductDTO productDTO = productMapper.productToProductDTO(product);

        if (productDTO.getImagePath() != null && !productDTO.getImagePath().isEmpty()) {
            try {
                // Usar el método utilitario del servicio S3
                String key = s3StorageService.extractKeyFromUrl(productDTO.getImagePath());

                if (key != null) {
                    String signedUrl = s3StorageService.generatePresignedUrl(key, DEFAULT_URL_DURATION);
                    productDTO.setImagePath(signedUrl);
                }

            } catch (Exception e) {
                System.err.println("Error generando URL firmada para producto " +
                        product.getReference() + ": " + e.getMessage());
                // Mantener URL original como fallback
            }
        }

        return productDTO;
    }

}
