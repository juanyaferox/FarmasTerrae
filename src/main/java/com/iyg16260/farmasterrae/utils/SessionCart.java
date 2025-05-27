package com.iyg16260.farmasterrae.utils;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SessionCart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Posible mejora, usar ProductoDTO dentro de un CartDTO para asi tener la info del producto en caché

    // Usando MAP puedo evitar tener que recorrer la lista contando los productos para conocer la cantidad
    private final Map<String, Integer> products = new HashMap<>();

    /**
     * Añade producto a la cesta, aumentando en uno la cantidad si está repetido
     *
     * @param productReference referencia del producto
     */
    public void addProduct(String productReference) {
        products.merge(productReference, 1, Integer::sum);
    }

    /**
     * Borra una unidad del producto del Map
     *
     * @param productReference referencia del producto
     */
    public void deleteOneProduct(String productReference) {
        products.merge(productReference, 1, (oldQuantity, value) -> {
            int newQuantity = oldQuantity - 1;
            return newQuantity > 0 ? newQuantity : null;
        });
    }

    /**
     * Borra todas las unidades del producto del Map
     *
     * @param productReference referencia del producto
     */
    public void deleteAllSameProduct(String productReference) {
        products.remove(productReference);
    }

    /**
     * Borra todo_ el contendido del carrito
     */
    public void clear() {
        products.clear();
    }

    /**
     * Obtiene todos los productos del carrito
     *
     * @return productos por referencia y cantidad
     */
    public Map<String, Integer> getProducts() {
        return products;
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
