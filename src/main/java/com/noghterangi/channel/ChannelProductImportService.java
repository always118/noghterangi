package com.noghterangi.channel;

import com.noghterangi.category.Category;
import com.noghterangi.category.CategoryRepository;
import com.noghterangi.media.MediaService;
import com.noghterangi.media.MediaStorageException;
import com.noghterangi.product.Product;
import com.noghterangi.product.ProductForm;
import com.noghterangi.product.ProductRepository;
import com.noghterangi.product.ProductService;
import com.noghterangi.product.ProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

/** Imports a reviewed channel manifest through the normal category, product and media services. */
@Service
public class ChannelProductImportService {
    private static final long MAX_MANIFEST_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PRODUCTS = 500;
    private static final int MAX_IMAGES_PER_PRODUCT = 10;

    private final CategoryRepository categoryRepository;
    private final ProductService products;
    private final ProductRepository productRepository;
    private final MediaService media;
    private final TransactionTemplate transaction;
    private final Path mediaRoot;
    private final JsonMapper json = JsonMapper.builder().build();

    public ChannelProductImportService(CategoryRepository categoryRepository,
            ProductService products, ProductRepository productRepository, MediaService media,
            PlatformTransactionManager transactions,
            @Value("${app.product-import.media-root:noghterangi-export}") String mediaRoot) {
        this.categoryRepository = categoryRepository;
        this.products = products;
        this.productRepository = productRepository;
        this.media = media;
        this.transaction = new TransactionTemplate(transactions);
        this.mediaRoot = Path.of(mediaRoot).toAbsolutePath().normalize();
    }

    public synchronized ImportReport importManifest(byte[] manifestBytes, boolean deleteExisting,
            boolean allowPriceReviewRequired) throws IOException {
        if (manifestBytes == null || manifestBytes.length == 0 || manifestBytes.length > MAX_MANIFEST_BYTES) {
            throw new IOException("Manifest is empty or too large");
        }
        JsonNode root = json.readTree(manifestBytes);
        JsonNode productRows = root.path("products");
        if (!productRows.isArray() || productRows.size() > MAX_PRODUCTS) {
            throw new IOException("Manifest must contain a bounded products array");
        }
        Path safeRoot = mediaRoot.toRealPath();
        List<String> notes = new ArrayList<>();
        Map<String, Category> categoryMap = resolveCategories(productRows, notes);
        int deleted = deleteExisting ? transaction.execute(status -> deleteAllProducts(notes)) : 0;
        int imported = 0, images = 0, skipped = 0, duplicates = 0, review = 0, missing = 0, rejected = 0;
        Set<String> keys = new HashSet<>();

        for (JsonNode row : productRows) {
            String slug = text(row, "slug");
            try {
                String name = text(row, "name");
                String sku = text(row, "sku");
                if (!keys.add("slug:" + slug) || !keys.add("sku:" + sku.toLowerCase(Locale.ROOT))
                        || !keys.add("name:" + name.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("duplicate key in manifest");
                }
                if (row.path("priceToman").asLong(0) <= 0) {
                    review++; skipped++; notes.add("Missing price; skipped: " + safe(slug)); continue;
                }
                if (row.path("priceReviewRequired").asBoolean(false) && !allowPriceReviewRequired) {
                    review++; skipped++; notes.add("Needs price review: " + safe(slug)); continue;
                }
                if (productRepository.existsBySlug(slug) || productRepository.existsBySku(sku)
                        || productRepository.existsByNameIgnoreCase(name)) {
                    duplicates++; notes.add("Existing product retained: " + safe(slug)); continue;
                }
                Category category = categoryMap.get(text(row, "categorySlug"));
                if (category == null || !category.isActive()) { throw new IllegalArgumentException("active category missing"); }
                List<MultipartFile> files = readImages(safeRoot, row.path("images"));
                ProductForm form = form(row, category.getId());
                transaction.executeWithoutResult(status -> {
                    Product product = products.create(form);
                    if (!files.isEmpty()) { media.upload(product.getId(), files); }
                });
                imported++; images += files.size();
                notes.add("Imported: " + safe(slug) + " (" + files.size() + " images)");
            } catch (NoSuchFileException exception) {
                skipped++; missing++; notes.add("Missing image; skipped: " + safe(slug));
            } catch (MediaStorageException exception) {
                skipped++; rejected++; notes.add("Rejected image; skipped: " + safe(slug));
            } catch (RuntimeException exception) {
                skipped++; notes.add("Skipped: " + safe(slug) + " (" + exception.getClass().getSimpleName() + ")");
            }
        }
        return new ImportReport(deleted, imported, images, skipped, duplicates, review, missing, rejected, notes);
    }

    /** The target database owns category names, parents and active state. */
    private Map<String, Category> resolveCategories(JsonNode productRows, List<String> notes) throws IOException {
        Map<String, Category> result = new HashMap<>();
        Set<String> slugs = new TreeSet<>();
        for (JsonNode row : productRows) {
            String slug = text(row, "categorySlug");
            if (slug.isBlank()) { throw new IOException("Product categorySlug is required"); }
            slugs.add(slug);
        }
        for (String slug : slugs) {
            result.put(slug, categoryRepository.findBySlug(slug).orElseThrow(() ->
                    new IOException("Category slug does not exist in target database: " + slug)));
        }
        notes.add("Categories resolved from target database: " + result.size());
        return result;
    }

    private int deleteAllProducts(List<String> notes) {
        int count = 0;
        for (Product product : new ArrayList<>(productRepository.findAll())) {
            media.images(product.getId()).forEach(image -> media.delete(product.getId(), image.getId()));
            productRepository.delete(product);
            count++;
        }
        productRepository.flush();
        notes.add("Deleted existing products explicitly: " + count);
        return count;
    }

    private ProductForm form(JsonNode row, Long categoryId) {
        ProductForm form = new ProductForm();
        form.setName(text(row, "name")); form.setSlug(text(row, "slug")); form.setSku(text(row, "sku"));
        form.setBrand(nullableText(row, "brand")); form.setShortDescription(nullableText(row, "shortDescription"));
        form.setDescription(nullableText(row, "description")); form.setPrice(row.path("priceToman").asLong());
        form.setStockQuantity(Math.max(0, row.path("stockQuantity").asInt(0)));
        form.setActive(row.path("active").asBoolean(true)); form.setFeatured(row.path("featured").asBoolean(false));
        form.setDisplayOrder(Math.max(0, row.path("displayOrder").asInt(0)));
        form.setType(ProductType.valueOf(row.path("type").asText("OTHER"))); form.setCategoryId(categoryId);
        return form;
    }

    private List<MultipartFile> readImages(Path root, JsonNode names) throws IOException {
        if (!names.isArray() || names.size() > MAX_IMAGES_PER_PRODUCT) { throw new IllegalArgumentException("invalid image list"); }
        List<MultipartFile> files = new ArrayList<>();
        for (JsonNode item : names) {
            Path path = root.resolve(item.asText()).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) { throw new NoSuchFileException(path.toString()); }
            byte[] bytes = Files.readAllBytes(path);
            files.add(new LocalImage(path.getFileName().toString(), bytes));
        }
        return files;
    }

    private String text(JsonNode row, String field) { return row.path(field).asText("").strip(); }
    private String nullableText(JsonNode row, String field) { String value = text(row, field); return value.isBlank() ? null : value; }
    private String safe(String value) { return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9-]", "_"); }

    public record ImportReport(int deleted, int imported, int images, int skipped, int duplicates,
                               int priceReviewRequired, int missingImages, int rejectedImages, List<String> notes) {}

    private record LocalImage(String name, byte[] bytes) implements MultipartFile {
        @Override public String getName() { return "files"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return name.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg"; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(java.io.File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
