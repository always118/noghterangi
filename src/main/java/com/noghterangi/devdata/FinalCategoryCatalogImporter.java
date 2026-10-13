package com.noghterangi.devdata;

import com.noghterangi.category.*;
import com.noghterangi.media.MediaService;
import com.noghterangi.product.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit local catalog entry point using the same services as admin flows. */
@Component
@Profile("(dev | test) & !prod & !production")
@ConditionalOnProperty(name = "app.category-catalog.enabled", havingValue = "true")
public class FinalCategoryCatalogImporter implements ApplicationRunner {
    private final CategoryService categories; private final CategoryRepository categoryRepository;
    private final ProductService products; private final MediaService media; private final Path root;
    private final Path catalogFile;
    private final JsonMapper json = JsonMapper.builder().build();

    public FinalCategoryCatalogImporter(CategoryService categories, CategoryRepository categoryRepository,
            ProductService products, MediaService media, @Value("${app.media.storage-path}") String storagePath,
            @Value("${app.category-catalog.file:dev-data/category-catalog.json}") String catalogFile) {
        this.categories = categories; this.categoryRepository = categoryRepository; this.products = products;
        this.media = media; this.root = Path.of(storagePath).toAbsolutePath().normalize();
        this.catalogFile = Path.of(catalogFile).toAbsolutePath().normalize();
    }

    @Override @Transactional public void run(ApplicationArguments args) throws Exception {
        Map<String, Category> finalCategories = createCategories();
        int moved = moveProducts(finalCategories);
        removeObsolete(finalCategories.keySet());
        assignImages(finalCategories);
        System.out.println("Final catalog imported through admin services; products reassigned: " + moved);
    }

    private Map<String, Category> createCategories() throws IOException {
        Map<String, Category> result = new LinkedHashMap<>();
        JsonNode roots = json.readTree(Files.readString(catalogFile)).path("roots");
        if (!roots.isArray() || roots.size() != 8) { throw new IOException("Canonical category catalog must contain 8 roots"); }
        for (JsonNode row : roots) {
            Category parent = getOrCreate(row.path("name").asText(), row.path("slug").asText(), null);
            result.put(row.path("slug").asText(), parent);
            for (JsonNode child : row.path("children")) {
                result.put(child.path("slug").asText(), getOrCreate(child.path("name").asText(),
                        child.path("slug").asText(), parent.getId()));
            }
        }
        return result;
    }

    private Category getOrCreate(String name, String slug, Long parentId) {
        Category category = categoryRepository.findBySlug(slug).orElse(null);
        if (category != null) {
            if (!category.isActive() || !Objects.equals(category.getParent() == null ? null : category.getParent().getId(), parentId)) {
                CategoryForm form = CategoryForm.from(category); form.setName(name); form.setSlug(slug);
                form.setActive(true); form.setParentId(parentId); return categories.update(category.getId(), form);
            }
            return category;
        }
        CategoryForm form = new CategoryForm(); form.setName(name); form.setSlug(slug); form.setActive(true); form.setParentId(parentId);
        return categories.create(form);
    }

    private int moveProducts(Map<String, Category> finalCategories) {
        int count = 0;
        for (Product product : products.findAll()) {
            String oldSlug = product.getCategory().getSlug(); if (finalCategories.containsKey(oldSlug)) continue;
            String target = switch (oldSlug) {
                case "dev-notebooks", "notebooks" -> "exercise-books";
                case "dev-pencils" -> "pencils";
                case "dev-bottles" -> "bottles-flasks";
                case "dev-stationery", "stationery-old" -> product.getName().contains("پاک‌کن") ? "erasers" : product.getName().contains("تخته") ? "math-tools" : "stationery";
                default -> "stationery";
            };
            ProductForm form = ProductForm.from(product); form.setCategoryId(finalCategories.get(target).getId());
            products.update(product.getId(), form); count++;
        }
        return count;
    }

    private void removeObsolete(Set<String> finalSlugs) {
        boolean changed; do { changed = false;
            for (Category category : new ArrayList<>(categoryRepository.findAll())) {
                if (finalSlugs.contains(category.getSlug()) || categoryRepository.existsByParentId(category.getId())) continue;
                media.categoryImage(category.getId()).ifPresent(image -> media.deleteCategoryImage(category.getId()));
                categoryRepository.delete(category); categoryRepository.flush(); changed = true;
            }
        } while (changed);
    }

    private void assignImages(Map<String, Category> categoriesBySlug) throws IOException {
        Set<Path> used = new HashSet<>();
        for (JsonNode row : json.readTree(Files.readString(catalogFile)).path("roots")) {
            String slug = row.path("slug").asText();
            Path source = findImage(slug, used);
            if (source == null) { System.out.println("Category image missing: " + slug); continue; }
            media.replaceCategoryImage(categoriesBySlug.get(slug).getId(), new LocalImage(source.getFileName().toString(), Files.readAllBytes(source)));
            used.add(source); System.out.println("Category image assigned: " + slug + " <- " + source.getFileName());
        }
    }

    private Path findImage(String slug, Set<Path> used) throws IOException {
        if (!Files.isDirectory(root)) return null;
        try (var files = Files.list(root)) {
            return files.filter(Files::isRegularFile).filter(p -> !used.contains(p)).filter(p -> supported(p.getFileName().toString()))
                    .filter(p -> base(p.getFileName().toString()).equals(slug) || base(p.getFileName().toString()).startsWith(slug + "-"))
                    .sorted(Comparator.<Path, Boolean>comparing(p -> !base(p.getFileName().toString()).equals(slug))
                            .thenComparing(p -> p.getFileName().toString())).findFirst().orElse(null);
        }
    }
    private boolean supported(String name) { String s = name.toLowerCase(Locale.ROOT); return s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png"); }
    private String base(String name) { int dot = name.lastIndexOf('.'); return (dot < 0 ? name : name.substring(0, dot)).toLowerCase(Locale.ROOT); }

    private record LocalImage(String name, byte[] bytes) implements MultipartFile {
        public String getName() { return "image"; } public String getOriginalFilename() { return name; }
        public String getContentType() { return name.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg"; }
        public boolean isEmpty() { return bytes.length == 0; } public long getSize() { return bytes.length; }
        public byte[] getBytes() { return bytes; } public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
