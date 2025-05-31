package com.iyg16260.farmasterrae.spec;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public Specification<Product> searchInReferenceNameDescription(String keyword) {
        return GenericSpecification.likeIgnoreCase(new String[]{"reference", "name", "description"}, keyword);
    }

    public Specification<Product> searchByCategory(Category category) {
        return GenericSpecification.equal("category", category);
    }
}
