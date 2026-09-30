package com.noghterangi.devdata;

import com.noghterangi.category.*;
import com.noghterangi.product.*;
import com.noghterangi.media.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit, local development import. Never updates an existing product, category or image. */
@Component
@Profile("(dev | test) & !prod & !production")
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevelopmentDataImporter implements ApplicationRunner {
    private final CategoryService categories;
    private final CategoryRepository categoryRepository;
    private final ProductService products;
    private final ProductRepository productRepository;
    private final MediaService media;
    private final TransactionTemplate transaction;
    private final Path directory;
    private final JsonMapper json = JsonMapper.builder().build();

    public DevelopmentDataImporter(CategoryService categories, CategoryRepository categoryRepository,
            ProductService products, ProductRepository productRepository, MediaService media,
            PlatformTransactionManager transactions,
            @Value("${app.dev-data.directory:dev-data}") String directory) {
        this.categories = categories; this.categoryRepository = categoryRepository;
        this.products = products; this.productRepository = productRepository; this.media = media;
        this.transaction = new TransactionTemplate(transactions); this.directory = Path.of(directory);
    }

    @Override public void run(ApplicationArguments arguments) throws IOException { importData(); }

    public synchronized ImportResult importData() throws IOException {
        Path root = directory.toRealPath();
        Path manifest = root.resolve("products.json").toRealPath();
        if (!manifest.startsWith(root) || Files.size(manifest) > 1_048_576) {
            throw new IOException("Invalid development manifest path or size");
        }
        JsonNode data = json.readTree(Files.readString(manifest));
        if (!data.path("categories").isArray() || !data.path("products").isArray()
                || data.path("categories").size() > 100 || data.path("products").size() > 500) {
            throw new IOException("Development manifest must contain bounded categories and products arrays");
        }
        var notes = new ArrayList<String>();
        Set<String> acceptedCategories = new HashSet<>();
        int categoryCount = 0, imported = 0, images = 0, skipped = 0, duplicates = 0, missing = 0, rejected = 0;
        Set<String> categoryConflicts = conflicts(data.path("categories"), false);
        for (JsonNode row : data.path("categories")) {
            try {
                var form = json.treeToValue(row, CategoryForm.class);
                if (hasConflict(row, false, categoryConflicts)) { throw new IllegalArgumentException("conflicting category"); }
                var existing = categoryRepository.findBySlug(form.getSlug());
                if (existing.isPresent()) {
                    if (existing.get().getName().equals(form.getName()) && existing.get().isActive()) {
                        acceptedCategories.add(form.getSlug());
                    }
                    notes.add("Category retained (no overwrite): " + safe(form.getSlug())); continue;
                }
                if (categoryRepository.existsByNameIgnoreCase(form.getName())) {
                    notes.add("Category name already exists; no duplicate created: " + safe(form.getSlug())); continue;
                }
                categories.create(form); categoryCount++;
                if (form.isActive()) { acceptedCategories.add(form.getSlug()); }
            } catch (RuntimeException exception) {
                notes.add("Category skipped: " + safe(row.path("slug").asText()) + " (" + exception.getClass().getSimpleName() + ")");
            }
        }
        Set<String> productConflicts = conflicts(data.path("products"), true);
        for (JsonNode row : data.path("products")) {
            String slug = row.path("form").path("slug").asText();
            try {
                if (hasConflict(row, true, productConflicts)) { throw new IllegalArgumentException("conflicting product"); }
                var input = json.treeToValue(row, InputProduct.class);
                var form = input.form();
                if (productRepository.existsBySlug(form.getSlug()) || productRepository.existsBySku(form.getSku())
                        || productRepository.existsByNameIgnoreCase(form.getName())) {
                    duplicates++; notes.add("Product retained (no overwrite): " + safe(slug)); continue;
                }
                if (!acceptedCategories.contains(input.categorySlug())) { throw new IllegalArgumentException("category conflict"); }
                var category = categoryRepository.findBySlugAndActiveTrue(input.categorySlug())
                        .orElseThrow(() -> new IllegalArgumentException("active category missing"));
                form.setCategoryId(category.getId());
                List<MultipartFile> files = new ArrayList<>();
                if (input.images() == null || input.images().size() > 10) { throw new IllegalArgumentException("invalid image list"); }
                for (String name : input.images()) { files.add(readImage(root, name)); }
                transaction.executeWithoutResult(status -> {
                    var product = products.create(form);
                    if (!files.isEmpty()) { media.upload(product.getId(), files); }
                });
                imported++; images += files.size();
                notes.add("Product imported: " + safe(slug) + " (" + files.size() + " images)");
            } catch (NoSuchFileException exception) {
                skipped++; missing++; notes.add("Product skipped; missing image: " + safe(slug));
            } catch (MediaStorageException | InvalidImageException exception) {
                skipped++; rejected++; notes.add("Product skipped; rejected image: " + safe(slug));
            } catch (IOException | RuntimeException exception) {
                skipped++; notes.add("Product skipped: " + safe(slug) + " (" + exception.getClass().getSimpleName() + ")");
            }
        }
        var result = new ImportResult(categoryCount, imported, images, skipped, duplicates, missing, rejected);
        String report = "# Development import report\n\nRun: " + java.time.Instant.now()
                + "\n\nSample prices were explicitly authorized by the user; these are NOT confirmed retail prices."
                + " Unknown inventory remains zero. See data-review.md for source conflicts and exclusions.\n\n"
                + "- Categories created: " + categoryCount + "\n- Products imported: " + imported
                + "\n- Images imported: " + images + "\n- Products skipped: " + skipped + "\n- Existing products retained: " + duplicates
                + "\n- Missing image failures: " + missing + "\n- Rejected image failures: " + rejected + "\n\n"
                + String.join("\n", notes.stream().map(note -> "- " + note).toList()) + "\n";
        // Never follow a report symlink outside the selected development directory.
        Files.writeString(root.resolve("import-report.md"), report, LinkOption.NOFOLLOW_LINKS,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return result;
    }

    private MultipartFile readImage(Path root, String name) throws IOException {
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root)) { throw new InvalidImageException(); }
        path = path.toRealPath();
        if (!path.startsWith(root) || !Files.isRegularFile(path) || Files.size(path) > LocalMediaStorageService.MAX_BYTES) {
            throw new InvalidImageException();
        }
        byte[] bytes;
        try (var input = Files.newInputStream(path)) { bytes = input.readNBytes(LocalMediaStorageService.MAX_BYTES + 1); }
        if (bytes.length > LocalMediaStorageService.MAX_BYTES) { throw new InvalidImageException(); }
        return new LocalImage(path.getFileName().toString(), bytes);
    }

    // Reject all rows sharing a key within a manifest, rather than choosing a conflicting first/last value.
    private Set<String> conflicts(JsonNode rows, boolean products) {
        Set<String> seen = new HashSet<>(), conflicts = new HashSet<>();
        for (JsonNode row : rows) {
            for (String key : keys(row, products)) { if (!seen.add(key)) { conflicts.add(key); } }
        }
        return conflicts;
    }
    private boolean hasConflict(JsonNode row, boolean products, Set<String> conflicts) {
        return keys(row, products).stream().anyMatch(conflicts::contains);
    }
    private List<String> keys(JsonNode row, boolean products) {
        JsonNode form = products ? row.path("form") : row;
        return (products ? List.of("name", "slug", "sku") : List.of("name", "slug")).stream()
                .map(field -> field + ":" + form.path(field).asText().strip().toLowerCase(Locale.ROOT)).toList();
    }
    private String safe(String value) { return value.replaceAll("[^a-zA-Z0-9-]", "_"); }

    public record InputProduct(ProductForm form, String categorySlug, List<String> images) {}
    public record ImportResult(int categories, int products, int images, int skipped, int duplicates, int missingImages, int rejectedImages) {}
    private static class InvalidImageException extends IOException {}
    private record LocalImage(String name, byte[] bytes) implements MultipartFile {
        @Override public String getName() { return "files"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return name.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg"; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
