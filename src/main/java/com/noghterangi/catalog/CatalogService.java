package com.noghterangi.catalog;

import com.noghterangi.category.CategoryRepository;
import com.noghterangi.product.Product;
import com.noghterangi.product.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products; this.categories = categories;
    }

    public Page<Product> search(CatalogFilter filter) {
        var pageable = PageRequest.of(filter.getPage(), CatalogFilter.PAGE_SIZE, filter.getSort().databaseSort());
        if (filter.isInvalid()) { return Page.empty(pageable); }
        Long categoryId = null;
        if (!filter.getCategory().isEmpty()) {
            var selected = categories.findBySlugAndActiveTrue(filter.getCategory());
            if (selected.isEmpty()) {
                filter.reject("دسته‌بندی فعال پیدا نشد. دسته دیگری انتخاب کنید.");
                return Page.empty(pageable);
            }
            categoryId = selected.get().getId();
        }
        Long selectedId = categoryId;
        String normalized = PersianTextNormalizer.normalize(filter.getQ()).toLowerCase(Locale.ROOT);
        // Escape LIKE metacharacters: user input is literal text, never a SQL wildcard or expression.
        String pattern = "%" + normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Specification<Product> specification = (root, query, cb) -> {
            var category = root.join("category");
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isTrue(category.get("active")));
            if (selectedId != null) {
                predicates.add(cb.or(cb.equal(category.get("id"), selectedId), cb.equal(category.get("parent").get("id"), selectedId)));
            }
            if (!normalized.isEmpty()) {
                predicates.add(cb.like(cb.lower(cb.function("noghterangi.normalize_persian", String.class, root.get("name"))), pattern, '!'));
            }
            if (filter.getMinimum() != null) { predicates.add(cb.ge(root.get("price"), filter.getMinimum())); }
            if (filter.getMaximum() != null) { predicates.add(cb.le(root.get("price"), filter.getMaximum())); }
            if (filter.isAvailable()) { predicates.add(cb.gt(root.get("stockQuantity"), 0)); }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return products.findAll(specification, pageable);
    }
}
