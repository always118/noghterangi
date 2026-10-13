package com.noghterangi.catalog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Explicitly enabled development/test catalog administration API. */
@RestController
@RequestMapping("/admin/catalog")
@Profile("!prod & !production")
@ConditionalOnProperty(name = "app.catalog-management.enabled", havingValue = "true")
public class AdminCatalogManagementController {
    private final CatalogManagementService catalog;

    public AdminCatalogManagementController(CatalogManagementService catalog) { this.catalog = catalog; }

    @PostMapping(value = "/products/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    public CatalogManagementService.ProductResetReport clearProducts(
            @RequestParam String confirmation) {
        return catalog.deleteAllProducts(confirmation);
    }

    @PostMapping(value = "/categories/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    public CatalogManagementService.CategoryResetReport clearCategories(
            @RequestParam String confirmation) {
        return catalog.deleteAllCategories(confirmation);
    }

    @PostMapping(value = "/categories/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogManagementService.CategoryImportReport> importCategories(
            @RequestPart("catalog") MultipartFile catalogFile) throws IOException {
        if (catalogFile == null || catalogFile.isEmpty()) { return ResponseEntity.badRequest().build(); }
        return ResponseEntity.ok(catalog.importCategories(catalogFile.getBytes()));
    }
}
