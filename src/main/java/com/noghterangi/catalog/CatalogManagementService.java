package com.noghterangi.catalog;

import com.noghterangi.category.Category;
import com.noghterangi.category.CategoryForm;
import com.noghterangi.category.CategoryRepository;
import com.noghterangi.category.CategoryService;
import com.noghterangi.media.Media;
import com.noghterangi.media.MediaRepository;
import com.noghterangi.media.MediaStorageService;
import com.noghterangi.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.*;

/** Development/test catalog reset and canonical category import operations. */
@Service
public class CatalogManagementService {
    private static final Logger log = LoggerFactory.getLogger(CatalogManagementService.class);
    private static final String CONFIRMATION = "DELETE_ALL_CATALOG";

    private final CategoryRepository categories;
    private final CategoryService categoryService;
    private final ProductRepository products;
    private final MediaRepository media;
    private final MediaStorageService storage;
    private final JsonMapper json = JsonMapper.builder().build();

    public CatalogManagementService(CategoryRepository categories, CategoryService categoryService,
            ProductRepository products, MediaRepository media, MediaStorageService storage) {
        this.categories = categories;
        this.categoryService = categoryService;
        this.products = products;
        this.media = media;
        this.storage = storage;
    }

    @Transactional
    public ProductResetReport deleteAllProducts(String confirmation) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("Explicit catalog deletion confirmation is required.");
        }

        List<Media> existingMedia = media.findAll().stream()
                .filter(item -> item.getProduct() != null).toList();
        int productCount = (int) products.count();
        media.deleteAllInBatch(existingMedia);
        products.deleteAllInBatch();

        List<String> fileNames = existingMedia.stream().map(Media::getStoredFileName).toList();
        afterCommit(() -> fileNames.forEach(this::deleteFileSafely));
        return new ProductResetReport(productCount, existingMedia.size());
    }

    @Transactional
    public CategoryResetReport deleteAllCategories(String confirmation) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("Explicit catalog deletion confirmation is required.");
        }
        long remainingProducts = products.count();
        if (remainingProducts > 0) {
            throw new IllegalStateException("Delete products before deleting categories.");
        }

        List<Media> existingMedia = media.findAll().stream()
                .filter(item -> item.getCategory() != null).toList();
        List<Category> existingCategories = categories.findAll();
        int categoryCount = existingCategories.size();
        media.deleteAllInBatch(existingMedia);

        // The self-reference requires children to be removed before roots.
        List<Category> children = existingCategories.stream()
                .filter(category -> category.getParent() != null).toList();
        List<Category> roots = existingCategories.stream()
                .filter(category -> category.getParent() == null).toList();
        categories.deleteAllInBatch(children);
        categories.deleteAllInBatch(roots);

        List<String> fileNames = existingMedia.stream().map(Media::getStoredFileName).toList();
        afterCommit(() -> fileNames.forEach(this::deleteFileSafely));
        return new CategoryResetReport(categoryCount, existingMedia.size());
    }

    @Transactional
    public CategoryImportReport importCategories(byte[] catalogBytes) throws IOException {
        if (catalogBytes == null || catalogBytes.length == 0 || catalogBytes.length > 512 * 1024) {
            throw new IOException("Category catalog is empty or too large.");
        }
        JsonNode root = json.readTree(catalogBytes);
        JsonNode roots = root.path("roots");
        if (!roots.isArray() || roots.size() > 100) {
            throw new IOException("Catalog must contain a bounded roots array.");
        }

        Map<String, JsonNode> rootRows = new LinkedHashMap<>();
        Map<String, JsonNode> childRows = new LinkedHashMap<>();
        for (JsonNode parent : roots) {
            String parentSlug = slug(parent);
            requireCategoryFields(parent, parentSlug);
            if (rootRows.put(parentSlug, parent) != null) {
                throw new IOException("Duplicate root category slug: " + parentSlug);
            }
            JsonNode children = parent.path("children");
            if (!children.isArray() || children.size() > 100) {
                throw new IOException("Each root must contain a bounded children array.");
            }
            for (JsonNode child : children) {
                String childSlug = slug(child);
                requireCategoryFields(child, childSlug);
                if (rootRows.containsKey(childSlug) || childRows.put(childSlug, child) != null) {
                    throw new IOException("Duplicate category slug: " + childSlug);
                }
            }
        }

        int created = 0;
        int existing = 0;
        Map<String, Category> resolvedRoots = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : rootRows.entrySet()) {
            Category category = categories.findBySlug(entry.getKey()).orElse(null);
            if (category == null) {
                category = categoryService.create(form(entry.getValue(), null));
                created++;
            } else {
                existing++;
            }
            resolvedRoots.put(entry.getKey(), category);
        }
        for (JsonNode parent : roots) {
            String parentSlug = slug(parent);
            for (JsonNode child : parent.path("children")) {
                String childSlug = slug(child);
                Category category = categories.findBySlug(childSlug).orElse(null);
                if (category == null) {
                    categoryService.create(form(child, resolvedRoots.get(parentSlug).getId()));
                    created++;
                } else {
                    if (category.getParent() == null || !category.getParent().getId().equals(resolvedRoots.get(parentSlug).getId())) {
                        throw new IOException("Existing category has a different parent: " + childSlug);
                    }
                    existing++;
                }
            }
        }
        return new CategoryImportReport(created, existing, rootRows.size(), childRows.size());
    }

    private CategoryForm form(JsonNode row, Long parentId) {
        CategoryForm form = new CategoryForm();
        form.setName(row.path("name").asText().strip());
        form.setSlug(row.path("slug").asText().strip());
        form.setActive(true);
        form.setParentId(parentId);
        return form;
    }

    private String slug(JsonNode row) { return row.path("slug").asText("").strip().toLowerCase(Locale.ROOT); }

    private void requireCategoryFields(JsonNode row, String slug) throws IOException {
        if (row.path("name").asText("").strip().isBlank()
                || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IOException("Invalid category name or slug: " + slug);
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    private void deleteFileSafely(String name) {
        try { storage.delete(name); }
        catch (RuntimeException exception) { log.warn("Catalog media cleanup failed for {}", name); }
    }

    public static String confirmationToken() { return CONFIRMATION; }

    public record ProductResetReport(int deletedProducts, int deletedMedia) {}
    public record CategoryResetReport(int deletedCategories, int deletedMedia) {}
    public record CategoryImportReport(int created, int existing, int roots, int children) {}
}
