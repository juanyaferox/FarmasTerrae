package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductsService {

    private final int PAGE_SIZE = 20;

    public Page<Product> getProductList(int page){
        return new PageImpl<>(List.of());
    }

}
