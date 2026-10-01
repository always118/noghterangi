package com.noghterangi.home;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import com.noghterangi.category.*;
import com.noghterangi.product.*;
import com.noghterangi.media.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class HomeIntegrationTest {
    @Autowired HomeService home;
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired MediaService media;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private final String prefix = "home-" + UUID.randomUUID();
    private final List<Long> productIds = new ArrayList<>(), categoryIds = new ArrayList<>();
    private Category root;
    private TestBrowser browser;

    @BeforeEach void setUp() { browser = new TestBrowser(port); root = category("root", null); }
    @AfterEach void cleanUp() {
        browser.close();
        for (Long id : productIds) {
            media.images(id).forEach(image -> media.delete(id, image.getId()));
            productRepository.deleteById(id);
        }
        categoryIds.reversed().forEach(categoryRepository::deleteById);
    }

    @Test void anonymousHomepageUsesActiveRootsAndWorkingCatalogSearchLinks() throws Exception {
        Category child = category("child", root.getId()), hidden = category("hidden", null);
        categories.setActive(hidden.getId(), false);
        var response = browser.get("/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("dir=\"rtl\"", "id=\"category-" + root.getSlug(),
                "href=\"/products?category=" + root.getSlug(), "action=\"/products\"", "method=\"get\"", "name=\"q\"",
                "دنیایی از", "رنگ، فکر و خیال", "/images/brand-logo.jpg", "id=\"cart-count\"", "href=\"/products#catalog-q\"", "/js/public-ux.js")
                .doesNotContain("id=\"category-" + child.getSlug(), "id=\"category-" + hidden.getSlug(),
                        "نمونه‌های نمایشی", "خرید آنلاین در مرحله‌های بعدی");
    }

    @Test void allSectionsUseDatabaseLimitAndStableNewestOrder() {
        for (int i = 0; i < 11; i++) {
            Product product = product(root, ProductType.BOARD_GAME, true);
            jdbc.update("update noghterangi.product set created_at = timestamp with time zone '2090-01-01 00:00:00+00' where id = ?", product.getId());
        }
        var expected = productIds.reversed().subList(0, 8);
        var content = home.content();
        assertThat(content.newest()).extracting(Product::getId).containsExactlyElementsOf(expected);
        assertThat(content.featured()).extracting(Product::getId).containsExactlyElementsOf(expected);
        assertThat(content.games()).extracting(Product::getId).containsExactlyElementsOf(expected);
        jdbc.update("update noghterangi.product set created_at = timestamp with time zone '2091-01-01 00:00:00+00' where id = ?", productIds.getFirst());
        assertThat(home.content().newest().getFirst().getId()).isEqualTo(productIds.getFirst());
    }

    @Test void everySectionRequiresActiveProductAndItsOwnActiveCategory() {
        Category hidden = category("hidden", null), child = category("child", hidden.getId());
        Product visible = product(root, ProductType.BOARD_GAME, true);
        Product inactive = product(root, ProductType.BOARD_GAME, true);
        Product hiddenCategory = product(hidden, ProductType.BOARD_GAME, true);
        Product activeChild = product(child, ProductType.BOARD_GAME, true);
        products.setActive(inactive.getId(), false); categories.setActive(hidden.getId(), false);
        var content = home.content();
        for (var section : List.of(content.newest(), content.featured(), content.games())) {
            assertThat(section).extracting(Product::getId).contains(visible.getId(), activeChild.getId())
                    .doesNotContain(inactive.getId(), hiddenCategory.getId());
        }
    }

    @Test void emptyOptionalSectionsDisappearAndTypesDoNotLeakIntoGames() throws Exception {
        Product plain = product(root, ProductType.STATIONERY, false);
        String body = browser.get("/").body();
        assertThat(body).contains("/products/" + plain.getSlug()).doesNotContain("id=\"featured-products\"", "id=\"board-games\"");
        Product game = product(root, ProductType.BOARD_GAME, true);
        assertThat(home.content().games()).extracting(Product::getId).containsExactly(game.getId());
        assertThat(home.content().featured()).extracting(Product::getId).containsExactly(game.getId());
        assertThat(browser.get("/").body()).contains("id=\"featured-products\"", "id=\"board-games\"");
    }

    @Test void cardsReusePrimaryImagesPlaceholderPriceAndAvailability() throws Exception {
        Product pictured = product(root, ProductType.STATIONERY, true);
        product(root, ProductType.STATIONERY, false);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var file = new MockMultipartFile("files", "test.png", "image/png", bytes.toByteArray());
        media.upload(pictured.getId(), List.of(file, file));
        var images = media.images(pictured.getId()); media.reorder(pictured.getId(), images.getFirst().getId(), 10);
        String body = browser.get("/").body();
        assertThat(body).contains("/media/" + images.getLast().getId(), "بدون تصویر", "ناموجود", "12,345", "ویژه", "/js/catalog-images.js")
                .doesNotContain("/media/" + images.getFirst().getId());
        assertThat(browser.get("/products/" + pictured.getSlug()).statusCode()).isEqualTo(200);
    }

    private Category category(String suffix, Long parent) {
        var form = new CategoryForm(); form.setName(prefix + suffix); form.setSlug(prefix + "-" + suffix); form.setParentId(parent);
        var result = categories.create(form); categoryIds.add(result.getId()); return result;
    }
    private Product product(Category category, ProductType type, boolean featured) {
        var form = new ProductForm(); form.setName(prefix + productIds.size()); form.setSlug(prefix + "-" + productIds.size());
        form.setSku(prefix + "-" + productIds.size()); form.setPrice(12345L); form.setCategoryId(category.getId());
        form.setType(type); form.setFeatured(featured);
        var result = products.create(form); productIds.add(result.getId()); return result;
    }
}
