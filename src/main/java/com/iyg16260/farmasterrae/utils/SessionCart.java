package com.iyg16260.farmasterrae.utils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Usando MAP puedo evitar tener que recorrer la lista contando los productos para conocer la cantidad
    private final Map<String, Integer> products = new ConcurrentHashMap<>();

    /**
     * Añade producto a la cesta, aumentando en uno la cantidad si está repetido
     *
     * @param productReference referencia del producto
     */
    public void addProduct(String productReference) {
        if (productReference == null) {
            return;
        }
        products.merge(productReference, 1, Integer::sum);
    }

    /**
     * Borra una unidad del producto del Map solo si está presente
     *
     * @param productReference referencia del producto
     */
    public void deleteOneProduct(String productReference) {
        if (productReference == null) {
            return;
        }
        products.computeIfPresent(productReference, (k, qty) -> qty > 1 ? qty - 1 : null);
    }

    /**
     * Borra todas las unidades del producto del Map
     *
     * @param productReference referencia del producto
     */
    public void deleteAllSameProduct(String productReference) {
        if (productReference == null) {
            return;
        }
        products.remove(productReference);
    }

    /**
     * Comprueba si el producto está en el carrito
     *
     * @param productReference referencia del producto
     * @return true si el producto está presente
     */
    public boolean containsProduct(String productReference) {
        return productReference != null && products.containsKey(productReference);
    }

    /**
     * Borra todo el contenido del carrito
     */
    public void clear() {
        products.clear();
    }

    /**
     * Obtiene una copia defensiva inmutable de todos los productos del carrito
     *
     * @return productos por referencia y cantidad
     */
    public Map<String, Integer> getProducts() {
        return Collections.unmodifiableMap(new TreeMap<>(products));
    }

    /**
     * Obtiene el tamaño del carrito
     *
     * @return tamaño del carrito
     */
    public int getSize() {
        return products.size();
    }
}
