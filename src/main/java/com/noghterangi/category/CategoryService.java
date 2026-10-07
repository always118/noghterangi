package com.noghterangi.category;

import jakarta.validation.Valid;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) { this.repository = repository; }

    public List<Category> findRoots() { return repository.findByParentIsNullOrderByNameAscIdAsc(); }
    public List<Category> findChildren(Long parentId) { return repository.findByParentIdOrderByNameAscIdAsc(parentId); }
    public List<Category> findAll() { return repository.findAllByOrderByNameAscIdAsc(); }
    public List<Category> findActive() { return repository.findByActiveTrueOrderByNameAscIdAsc(); }
    public Category get(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "دسته‌بندی پیدا نشد."));
    }

    @Transactional
    public Category create(@Valid CategoryForm form) {
        if (repository.existsBySlug(form.getSlug())) { throw new DuplicateCategorySlugException(); }
        Category category = new Category();
        category.setParent(resolveParent(form.getParentId(), category.getId()));
        category.update(form);
        return save(category);
    }

    @Transactional
    public Category update(Long id, @Valid CategoryForm form) {
        Category category = get(id);
        if (repository.existsBySlugAndIdNot(form.getSlug(), id)) { throw new DuplicateCategorySlugException(); }
        category.setParent(resolveParent(form.getParentId(), category.getId()));
        category.update(form);
        return save(category);
    }

    @Transactional
    public void setActive(Long id, boolean active) { get(id).setActive(active); }

    private Category resolveParent(Long parentId, Long categoryId) {
        if (parentId == null) { return null; }
        if (parentId.equals(categoryId)) {
            throw new InvalidCategoryParentException("دسته‌بندی نمی‌تواند والد خودش باشد.");
        }
        Category parent = repository.findById(parentId).orElseThrow(() ->
                new InvalidCategoryParentException("دسته‌بندی والد پیدا نشد."));
        if (parent.getParent() != null || (categoryId != null && repository.existsByParentId(categoryId))) {
            throw new InvalidCategoryParentException("فقط دو سطح دسته‌بندی مجاز است؛ والد باید دسته اصلی باشد و این دسته فرزند نداشته باشد.");
        }
        return parent;
    }

    private Category save(Category category) {
        try {
            return repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            // The unique constraint also protects concurrent requests after the friendly pre-check.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uk_category_slug".equals(violation.getConstraintName())) {
                    throw new DuplicateCategorySlugException();
                }
            }
            throw exception;
        }
    }
}
