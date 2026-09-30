package com.noghterangi.devdata;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.category.*;
import com.noghterangi.product.*;
import com.noghterangi.media.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class DevelopmentDataIntegrationTest {
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired MediaService media;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @TempDir Path directory;
    private final String prefix = "import-" + UUID.randomUUID();
    private DevelopmentDataImporter importer;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void setUp() throws Exception {
        importer = new DevelopmentDataImporter(categories, categoryRepository, products, productRepository, media, transactions, directory.toString());
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", directory.resolve("valid.png").toFile());
    }
    @AfterEach void cleanUp() {
        for (Long id : jdbc.queryForList("select id from noghterangi.product where slug like ?", Long.class, prefix + "%")) {
            media.images(id).forEach(image -> media.delete(id, image.getId())); productRepository.deleteById(id);
        }
        jdbc.update("delete from noghterangi.category where slug like ?", prefix + "%");
    }

    @Test void importsViaExistingMediaAndRepeatingNeverOverwritesOrDuplicates() throws Exception {
        manifest(List.of(row("one", 100L, List.of("valid.png"))));
        var result = importer.importData();
        assertThat(result.products()).isEqualTo(1); assertThat(result.images()).isEqualTo(1); assertThat(result.categories()).isEqualTo(1);
        Long id = jdbc.queryForObject("select id from noghterangi.product where slug = ?", Long.class, prefix + "-one");
        var product = products.get(id); assertThat(product.getStockQuantity()).isZero();
        var images = media.images(id); assertThat(images).hasSize(1);
        assertThat(images.getFirst().getStoredFileName()).endsWith(".png").isNotEqualTo("valid.png");
        assertThat(media.content(images.getFirst().getId()).bytes()).isEqualTo(Files.readAllBytes(directory.resolve("valid.png")));
        var edit = ProductForm.from(product); edit.setPrice(900L); products.update(id, edit);
        var repeated = importer.importData();
        assertThat(repeated.products()).isZero(); assertThat(repeated.images()).isZero(); assertThat(repeated.duplicates()).isEqualTo(1);
        assertThat(products.get(id).getPrice()).isEqualTo(900L); assertThat(media.images(id)).hasSize(1);
        assertThat(Files.readString(directory.resolve("import-report.md"))).contains("Products imported: 0", "Existing products retained: 1", "no overwrite");
    }

    @Test void invalidRecordsAndImagesAreIsolatedAndPartialMediaWritesRollBack() throws Exception {
        Files.writeString(directory.resolve("broken.png"), "not a photo");
        manifest(List.of(row("negative", -1L, List.of()), row("missing", 1L, List.of("missing.png")),
                row("corrupt", 1L, List.of("valid.png", "broken.png")), row("escape", 1L, List.of("../outside.png")),
                row("duplicate", 1L, List.of()), row("duplicate", 2L, List.of()), row("good", 1L, List.of("valid.png"))));
        var result = importer.importData();
        assertThat(result.products()).isEqualTo(1); assertThat(result.images()).isEqualTo(1); assertThat(result.skipped()).isEqualTo(6);
        assertThat(result.missingImages()).isEqualTo(1); assertThat(result.rejectedImages()).isEqualTo(2);
        assertThat(jdbc.queryForList("select slug from noghterangi.product where slug like ?", String.class, prefix + "%"))
                .containsExactly(prefix + "-good");
        assertThat(Files.readString(directory.resolve("import-report.md"))).contains("missing image", "rejected image", prefix + "-duplicate");
    }

    @Test void conflictingExistingCategoryIsNeverRepurposedAndMalformedManifestFailsBeforeWrites() throws Exception {
        var form = new CategoryForm(); form.setName("Existing different category " + prefix); form.setSlug(prefix + "-category");
        var existing = categories.create(form);
        manifest(List.of(row("one", 100L, List.of())));
        assertThat(importer.importData().skipped()).isEqualTo(1);
        assertThat(categories.get(existing.getId()).getName()).isEqualTo(form.getName());
        Files.writeString(directory.resolve("products.json"), "{\"categories\":[],\"products\":false}");
        assertThatThrownBy(importer::importData).isInstanceOf(java.io.IOException.class);
        assertThat(productRepository.existsBySlug(prefix + "-one")).isFalse();
    }

    private Map<String, Object> row(String suffix, long price, List<String> images) {
        return Map.of("form", Map.of("name", prefix + " " + suffix, "slug", prefix + "-" + suffix,
                        "sku", prefix + "-" + suffix, "price", price, "stockQuantity", 0, "active", true, "type", "STATIONERY"),
                "categorySlug", prefix + "-category", "images", images);
    }
    private void manifest(List<Map<String, Object>> rows) throws Exception {
        Files.writeString(directory.resolve("products.json"), json.writeValueAsString(Map.of(
                "categories", List.of(Map.of("name", prefix, "slug", prefix + "-category", "active", true)), "products", rows)));
    }
}
