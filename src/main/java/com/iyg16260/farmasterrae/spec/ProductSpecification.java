package com.iyg16260.farmasterrae.spec;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.model.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public Specification<Product> searchLike(String keyword) {
        return activeProducts(GenericSpecification.likeIgnoreCase(new String[]{"reference", "name", "description"}, keyword));
    }

    public Specification<Product> searchByCategory(Category category) {
        return activeProducts(GenericSpecification.equal("category", category));
    }

    public Specification<Product> searchLikeCategory(String keyword) {
        return Category.findByValueContainsIgnoreCase(keyword).stream()
                .map(this::searchByCategory)
                .reduce(Specification::or).orElse(null);
    }

    private Specification<Product> activeProducts(Specification<Product> filter) {
        return (root, query, cb) -> {
            Predicate active = cb.isNull(root.get("deletedAt"));
            Predicate requested = filter.toPredicate(root, query, cb);
            return requested == null ? active : cb.and(active, requested);
        };
    }
}
