package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.mapper.ProductMapper;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProductRepository;
import com.iyg16260.farmasterrae.spec.GenericSpecification;
import com.iyg16260.farmasterrae.spec.ProductSpecification;
import com.iyg16260.farmasterrae.utils.PageableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ProductsService {

    @Autowired
    S3StorageService s3StorageService;

    @Autowired
    ImageCompressionService compressionService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ReviewMapper reviewMapper;

    @Autowired
    ProductMapper productMapper;

    private final int PAGE_SIZE = 20;
    private final int PAGE_SIZE_ADMIN = 9;
    private final String PRODUCT_IMAGES_FOLDER = "product-images";

    /** Columnas por las que se puede ordenar el listado del panel de administración */
    static final Set<String> SORTABLE_FIELDS = Set.of("name", "reference", "category", "price", "stock");

    /**
     * Obtiene una página de productos para el panel de administración
     *
     * @param page    número de página; fuera de rango se ajusta a la primera o a la última
     * @param keyword texto de búsqueda; en blanco equivale a no buscar
     * @param sort    columna de ordenación; si no está en {@link #SORTABLE_FIELDS} se usa el orden por defecto
     * @param dir     dirección de la ordenación ({@code asc} o {@code desc})
     * @return página de productos DTO
     */
    public Page<ProductDTO> getProductList(int page, String keyword, String sort, String dir) {
        String search = GenericSpecification.normalizeKeyword(keyword);

        var specUtils = new ProductSpecification();
        Specification<Product> spec = search == null
                ? specUtils.searchLike(null)
                : specUtils.searchLike(search).or(specUtils.searchLikeCategory(search));

        return PageableUtils.findClampedPage(page, PAGE_SIZE_ADMIN, PageableUtils.sortOf(sort, dir, SORTABLE_FIELDS),
                        pageable -> productRepository.findAll(spec, pageable))
                .map(productMapper::productToProductDTO);
    }

    /**
     * Obtiene una página del catálogo público, opcionalmente filtrada por categoría y texto
     *
     * @param category categoría a filtrar, o {@code null} para todas
     * @param keyword  texto de búsqueda; en blanco equivale a no buscar
     * @param page     número de página; fuera de rango se ajusta a la primera o a la última
     * @return página de productos del catálogo
     */
    public Page<ProductPageDTO> getFilteredProducts(Category category, String keyword, int page) {

        ProductSpecification specUtil = new ProductSpecification();
        Specification<Product> spec = specUtil.searchByCategory(category)
                .and(specUtil.searchLike(GenericSpecification.normalizeKeyword(keyword)));

        return PageableUtils.findClampedPage(page, PAGE_SIZE, PageableUtils.defaultSort(),
                        pageable -> productRepository.findAll(spec, pageable))
                .map(productMapper::productToProductPageDTO);
    }

    /**
     * Obtiene un producto DTO por su referencia
     *
     * @param reference referencia del producto
     * @return producto DTO
     */
    public ProductDTO getProductDTOByReference(String reference) {
        Product product = getProductByReference(reference);

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
        return productRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    /**
     * Elimina un producto por su referencia
     *
     * @param reference referencia del producto a eliminar
     * @throws ResponseStatusException NOT_FOUND si no se encuentra el producto
     */
    public void deleteProduct(String reference) throws ResponseStatusException {
        Product product = getProductByReference(reference);
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    /**
     * Actualiza un producto existente
     *
     * @param productDTO   DTO con los datos actualizados del producto
     * @param oldReference referencia anterior del producto
     * @return producto DTO actualizado
     * @throws ResponseStatusException BAD_REQUEST si la nueva referencia ya existe, NOT_FOUND si no se encuentra el producto
     */
    @Transactional
    public ProductDTO updateProduct(ProductDTO productDTO, String oldReference, MultipartFile image) throws ResponseStatusException {
        Product existing = productRepository.findByReferenceAndDeletedAtIsNull(oldReference).orElseThrow(() -> new ResponseStatusException
                (HttpStatus.NOT_FOUND, "Producto no encontrado"));

        if (productDTO.getReference() != null
                && !productDTO.getReference().isBlank()
                && !productDTO.getReference().equals(oldReference)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La referencia del producto no se puede modificar");
        }

        existing.setName(productDTO.getName());
        existing.setDescription(productDTO.getDescription());
        if (productDTO.getPrice() != null) {
            existing.setPrice(productDTO.getPrice()
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .intValueExact());
        }
        if (productDTO.getStock() != null) {
            existing.setStock(productDTO.getStock());
        }
        existing.setCategory(productDTO.getCategory());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User currentUser && currentUser.getId() != null) {
            existing.setUserModified(currentUser);
        }

        String oldImagePath = existing.getImagePath();
        AtomicReference<String> uploadedPath = new AtomicReference<>();
        boolean syncRegistered = false;
        try {
            if (image != null && !image.isEmpty()) {
                MultipartFile imageToUpload = compressionService.compressImage(image);
                String newPath = s3StorageService.uploadFile(imageToUpload, PRODUCT_IMAGES_FOLDER);
                uploadedPath.set(newPath);
                existing.setImagePath(newPath);
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() {
                            if (oldImagePath != null) {
                                deleteImageQuietly(oldImagePath);
                            }
                        }

                        @Override public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                deleteImageQuietly(newPath);
                            }
                        }
                    });
                    syncRegistered = true;
                }
            }
            Product saved = productRepository.saveAndFlush(existing);
            if (!syncRegistered && uploadedPath.get() != null && oldImagePath != null) {
                deleteImageQuietly(oldImagePath);
            }
            return productMapper.productToProductDTO(saved);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es válida", e);
        } catch (RuntimeException e) {
            if (uploadedPath.get() != null && !syncRegistered) {
                deleteImageQuietly(uploadedPath.get());
            }
            throw e;
        }
    }

    /**
     * Guarda un nuevo producto en la base de datos
     *
     * @param productDTO DTO del producto a guardar
     * @return producto guardado
     * @throws ResponseStatusException BAD_REQUEST si ya existe un producto con esa referencia
     */
    @Transactional
    public Product saveProduct(ProductDTO productDTO, MultipartFile image) {
        Product product = productMapper.productDTOToProduct(productDTO);

        if (productRepository.existsByReference(product.getReference())) {
            throw new ResponseStatusException
                    (HttpStatus.BAD_REQUEST, "Ya existe un producto con esa referencia");
        }

        AtomicReference<String> uploadedPath = new AtomicReference<>();
        boolean syncRegistered = false;
        try {
            if (image != null && !image.isEmpty()) {
                MultipartFile compressed = compressionService.compressImage(image);
                String key = s3StorageService.uploadFile(compressed, PRODUCT_IMAGES_FOLDER);
                uploadedPath.set(key);
                product.setImagePath(key);
                syncRegistered = registerUploadRollbackCleanup(key);
            }
            return productRepository.saveAndFlush(product);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es válida", e);
        } catch (RuntimeException e) {
            if (uploadedPath.get() != null && !syncRegistered) {
                deleteImageQuietly(uploadedPath.get());
            }
            throw e;
        }
    }

    @Transactional
    public Product saveProduct(ProductDTO productDTO) {
        return saveProduct(productDTO, null);
    }

    private boolean registerUploadRollbackCleanup(String uploadedKey) {
        if (uploadedKey != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteImageQuietly(uploadedKey);
                    }
                }
            });
            return true;
        }
        return false;
    }

    private void deleteImageQuietly(String key) {
        try { s3StorageService.deleteFile(key); }
        catch (RuntimeException e) { log.warn("Could not clean up product image {}", key, e); }
    }

    /**
     * Obtiene una cantidad específica de productos aleatorios activos de la base de datos
     *
     * @param count número de productos a obtener
     * @return lista de productos aleatorios
     */
    public List<ProductDTO> getRandomProducts(int count) {
        if (count <= 0) {
            return List.of();
        }
        return productRepository.findRandomProducts(count).stream()
                .map(productMapper::productToProductDTO)
                .toList();
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
