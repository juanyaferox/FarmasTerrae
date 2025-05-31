package com.iyg16260.farmasterrae.spec;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public Specification<Product> searchLike(String keyword) {
        return GenericSpecification.likeIgnoreCase(new String[]{"reference", "name", "description"}, keyword);
    }

    public Specification<Product> searchByCategory(Category category) {
        return GenericSpecification.equal("category", category);
    }

    public Specification<Product> searchLikeCategory(String keyword) {
        return Category.findByValueContainsIgnoreCase(keyword).stream()
                .map(this::searchByCategory)
                .reduce(Specification::or).orElse(null);
    }
}
