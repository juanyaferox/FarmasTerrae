package com.iyg16260.farmasterrae.utils;

import com.iyg16260.farmasterrae.model.Product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartSession implements Serializable {

    private final List<Product> products = new ArrayList<>();

    public void addProduct(Product product) {
        products.add(product);
    }

    public void deleteOneProduct(long idProduct) {
        Iterator<Product> iterator = products.iterator();
        while(iterator.hasNext()) {
            var p = iterator.next();
            if (p.getId()==idProduct) {
                iterator.remove();
                break;
            }
        }
    }

    public void deleteAllSameProduct(long idProduct) {
        products.removeIf(p -> p.getId()==idProduct);
    }

    public void clear() {
        products.clear();
    }

    public List<Product> getProducts() {
        return products;
    }

    public int getSize() {
        return products.size();
    }
}
