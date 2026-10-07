package com.noghterangi.product;

import com.noghterangi.category.Category;
import com.noghterangi.category.CategoryRepository;
import jakarta.validation.Valid;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Validated
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    private final CategoryRepository categories;

    public ProductService(ProductRepository repository, CategoryRepository categories) {
        this.repository = repository;
        this.categories = categories;
    }

    public List<Product> findAll() { return repository.findAllByOrderByDisplayOrderAscIdAsc(); }
    public List<Product> findAll(Long categoryId) {
        return categoryId == null ? findAll() : repository.findByCategoryIdOrderByDisplayOrderAscIdAsc(categoryId);
    }
    public Product get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول پیدا نشد."));
    }

    public Product getPublic(String slug) {
        return repository.findBySlugAndActiveTrueAndCategoryActiveTrue(slug).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول پیدا نشد."));
    }

    public List<Category> categoryChoices(Long productId) {
        var choices = new ArrayList<>(categories.findByActiveTrueOrderByNameAscIdAsc());
        if (productId != null) {
            Category current = get(productId).getCategory();
            if (!current.isActive()) { choices.add(current); }
        }
        return choices;
    }

    public List<Category> categoryFilterChoices() { return categories.findAllByOrderByNameAscIdAsc(); }

    @Transactional
    public Product create(@Valid ProductForm form) {
        return save(new Product(), form);
    }

    @Transactional
    public Product update(Long id, @Valid ProductForm form) {
        return save(get(id), form);
    }

    @Transactional
    public void setActive(Long id, boolean active) { get(id).setActive(active); }

    private Product save(Product product, ProductForm form) {
        Long id = product.getId();
        if (id == null ? repository.existsBySlug(form.getSlug()) : repository.existsBySlugAndIdNot(form.getSlug(), id)) {
            throw duplicate("slug");
        }
        if (id == null ? repository.existsBySku(form.getSku()) : repository.existsBySkuAndIdNot(form.getSku(), id)) {
            throw duplicate("sku");
        }
        Category category = categories.findById(form.getCategoryId()).orElseThrow(() ->
                new ProductValidationException("categoryId", "دسته‌بندی پیدا نشد."));
        Long currentCategoryId = product.getCategory() == null ? null : product.getCategory().getId();
        if (!category.isActive() && !Objects.equals(category.getId(), currentCategoryId)) {
            throw new ProductValidationException("categoryId", "برای تخصیص جدید، یک دسته‌بندی فعال انتخاب کنید.");
        }
        product.update(form, category);
        try {
            return repository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            // Database uniqueness also protects simultaneous submissions.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation) {
                    if ("uk_product_slug".equals(violation.getConstraintName())) { throw duplicate("slug"); }
                    if ("uk_product_sku".equals(violation.getConstraintName())) { throw duplicate("sku"); }
                }
            }
            throw exception;
        }
    }

    private ProductValidationException duplicate(String field) {
        return new ProductValidationException(field, field.equals("slug")
                ? "این نامک قبلاً استفاده شده است." : "این کد کالا قبلاً استفاده شده است.");
    }
}
