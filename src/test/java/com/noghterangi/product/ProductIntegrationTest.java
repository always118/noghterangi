package com.noghterangi.product;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import com.noghterangi.category.Category;
import com.noghterangi.category.CategoryForm;
import com.noghterangi.category.CategoryRepository;
import com.noghterangi.category.CategoryService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class ProductIntegrationTest {
    @Autowired ProductService service;
    @Autowired ProductRepository repository;
    @Autowired CategoryService categoryService;
    @Autowired CategoryRepository categories;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private final String prefix = "product-test-" + UUID.randomUUID();
    private Category category;
    private TestBrowser browser;

    @BeforeEach
    void setUp() {
        browser = new TestBrowser(port);
        CategoryForm form = new CategoryForm();
        form.setName("دسته آزمون محصول");
        form.setSlug(prefix + "-category");
        category = categoryService.create(form);
    }

    @AfterEach
    void cleanUp() {
        browser.close();
        repository.deleteAll(repository.findAll().stream().filter(p -> p.getSlug().startsWith(prefix)).toList());
        categories.deleteAll(categories.findAll().stream().filter(c -> c.getSlug().startsWith(prefix)).toList());
    }

    @Test
    void createsProductWithExactLongTomanPriceCategoryAndTimestamps() {
        ProductForm form = form("one");
        form.setName("  دفتر رنگی  ");
        form.setSlug("  " + form.getSlug().toUpperCase(Locale.ROOT) + "  ");
        form.setSku("  " + form.getSku().toLowerCase(Locale.ROOT) + "  ");
        form.setPrice(3_500_000_000L);
        form.setBrand("برند نمونه");
        form.setShortDescription("توضیح کوتاه");
        form.setDescription("توضیحات کامل");
        Product saved = service.create(form);
        Product loaded = service.get(saved.getId());
        assertThat(loaded.getName()).isEqualTo("دفتر رنگی");
        assertThat(loaded.getSlug()).isEqualTo(prefix + "-one");
        assertThat(loaded.getSku()).isEqualTo((prefix + "-one").toUpperCase(Locale.ROOT));
        assertThat(loaded.getPrice()).isEqualTo(3_500_000_000L);
        assertThat(loaded.getCategory().getId()).isEqualTo(category.getId());
        assertThat(loaded.getCategory().getName()).isEqualTo("دسته آزمون محصول");
        assertThat(loaded.getType()).isEqualTo(ProductType.STATIONERY);
        assertThat(loaded.getBrand()).isEqualTo("برند نمونه");
        assertThat(loaded.getShortDescription()).isEqualTo("توضیح کوتاه");
        assertThat(loaded.getDescription()).isEqualTo("توضیحات کامل");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isEqualTo(loaded.getCreatedAt());
    }

    @Test
    void rejectsMissingFieldsNegativePriceAndNegativeStock() {
        List<Consumer<ProductForm>> invalidValues = List.of(
                f -> f.setName(" "), f -> f.setSlug(""), f -> f.setSku(" "),
                f -> f.setPrice(null), f -> f.setPrice(-1L),
                f -> f.setStockQuantity(null), f -> f.setStockQuantity(-1),
                f -> f.setCategoryId(null), f -> f.setType(null),
                f -> f.setSlug("invalid slug"), f -> f.setDisplayOrder(-1));
        for (var invalid : invalidValues) {
            ProductForm form = form("invalid");
            invalid.accept(form);
            assertThatThrownBy(() -> service.create(form)).isInstanceOf(ConstraintViolationException.class);
        }
        ProductForm zero = form("zero");
        zero.setPrice(0L);
        zero.setStockQuantity(0);
        assertThat(service.create(zero).getPrice()).isZero();
    }

    @Test
    void normalizesAndRejectsDuplicateSlugAndSkuIncludingInactiveProducts() {
        Product first = service.create(form("first"));
        service.setActive(first.getId(), false);
        ProductForm duplicateSlug = form("second");
        duplicateSlug.setSlug(" " + first.getSlug().toUpperCase(Locale.ROOT) + " ");
        assertThatThrownBy(() -> service.create(duplicateSlug)).isInstanceOf(ProductValidationException.class)
                .hasMessage("این نامک قبلاً استفاده شده است.");
        ProductForm duplicateSku = form("second");
        duplicateSku.setSku(" " + first.getSku().toLowerCase(Locale.ROOT) + " ");
        assertThatThrownBy(() -> service.create(duplicateSku)).isInstanceOf(ProductValidationException.class)
                .hasMessage("این کد کالا قبلاً استفاده شده است.");
        Product second = service.create(form("second"));
        assertThatThrownBy(() -> service.update(second.getId(), duplicateSlug)).isInstanceOf(ProductValidationException.class);
        assertThatThrownBy(() -> service.update(second.getId(), duplicateSku)).isInstanceOf(ProductValidationException.class);
        assertThat(service.get(second.getId()).getSlug()).isEqualTo(prefix + "-second");
        assertThat(service.get(second.getId()).getSku()).isEqualTo((prefix + "-second").toUpperCase(Locale.ROOT));
    }

    @Test
    void databaseEnforcesUniqueKeysNumericChecksAndCategoryForeignKey() {
        Product first = service.create(form("first"));
        Product second = service.create(form("second"));
        assertThatThrownBy(() -> jdbc.update("update noghterangi.product set slug = ? where id = ?", first.getSlug(), second.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update noghterangi.product set sku = ? where id = ?", first.getSku(), second.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update noghterangi.product set price = -1 where id = ?", first.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update noghterangi.product set stock_quantity = -1 where id = ?", first.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update noghterangi.product set category_id = ? where id = ?", Long.MAX_VALUE, first.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void categoryMustExistAndBeActiveForNewAssignmentButCanBeRetainedOnEdit() {
        Product product = service.create(form("existing"));
        categoryService.setActive(category.getId(), false);
        assertThat(service.categoryChoices(null)).extracting(Category::getId).doesNotContain(category.getId());
        assertThat(service.categoryChoices(product.getId())).extracting(Category::getId).contains(category.getId());
        ProductForm edit = ProductForm.from(service.get(product.getId()));
        edit.setPrice(250_000L);
        assertThat(service.update(product.getId(), edit).getPrice()).isEqualTo(250_000L);
        assertThatThrownBy(() -> service.create(form("inactive"))).isInstanceOf(ProductValidationException.class);
        ProductForm missing = form("missing");
        missing.setCategoryId(Long.MAX_VALUE);
        assertThatThrownBy(() -> service.create(missing)).isInstanceOf(ProductValidationException.class).hasMessage("دسته‌بندی پیدا نشد.");
    }

    @Test
    void validatesNullableGameFieldsAndClearsThemWhenTypeChanges() {
        ProductForm form = form("game");
        form.setType(ProductType.BOARD_GAME);
        Product game = service.create(form);
        assertThat(game.getMinimumAge()).isNull();
        List<Consumer<ProductForm>> invalidValues = List.of(
                f -> f.setMinimumAge(-1), f -> { f.setMinimumAge(8); f.setMaximumAge(7); },
                f -> f.setMinimumPlayers(0), f -> { f.setMinimumPlayers(3); f.setMaximumPlayers(2); },
                f -> f.setMaximumPlayers(0), f -> f.setPlayingTimeMinutes(0));
        for (var invalid : invalidValues) {
            ProductForm invalidForm = ProductForm.from(service.get(game.getId()));
            invalid.accept(invalidForm);
            assertThatThrownBy(() -> service.update(game.getId(), invalidForm)).isInstanceOf(ConstraintViolationException.class);
        }
        form.setMinimumAge(8); form.setMaximumAge(99);
        form.setMinimumPlayers(2); form.setMaximumPlayers(6); form.setPlayingTimeMinutes(30);
        service.update(game.getId(), form);
        Product loaded = service.get(game.getId());
        assertThat(loaded.getMinimumAge()).isEqualTo(8);
        assertThat(loaded.getMaximumAge()).isEqualTo(99);
        assertThat(loaded.getMinimumPlayers()).isEqualTo(2);
        assertThat(loaded.getMaximumPlayers()).isEqualTo(6);
        assertThat(loaded.getPlayingTimeMinutes()).isEqualTo(30);
        form.setType(ProductType.BOOK);
        service.update(game.getId(), form);
        loaded = service.get(game.getId());
        assertThat(loaded.getMinimumAge()).isNull();
        assertThat(loaded.getMaximumAge()).isNull();
        assertThat(loaded.getMinimumPlayers()).isNull();
        assertThat(loaded.getMaximumPlayers()).isNull();
        assertThat(loaded.getPlayingTimeMinutes()).isNull();
    }

    @Test
    void managesPriceStockFeaturedOrderAndActivationWithoutChangingCreationTime() {
        Product first = service.create(form("first"));
        Product second = service.create(form("second"));
        var createdAt = service.get(first.getId()).getCreatedAt();
        ProductForm edit = ProductForm.from(service.get(first.getId()));
        edit.setPrice(987_654L); edit.setStockQuantity(0); edit.setFeatured(true); edit.setDisplayOrder(5);
        service.update(first.getId(), edit);
        Product loaded = service.get(first.getId());
        assertThat(loaded.getPrice()).isEqualTo(987_654L);
        assertThat(loaded.getStockQuantity()).isZero();
        assertThat(loaded.isFeatured()).isTrue();
        assertThat(loaded.getDisplayOrder()).isEqualTo(5);
        assertThat(loaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(loaded.getUpdatedAt()).isAfter(createdAt);
        assertThat(service.findAll().stream().filter(p -> p.getSlug().startsWith(prefix)).map(Product::getId).toList())
                .containsExactly(second.getId(), first.getId());
        service.setActive(first.getId(), false);
        assertThat(service.get(first.getId()).isActive()).isFalse();
        service.setActive(first.getId(), true);
        assertThat(service.get(first.getId()).isActive()).isTrue();
    }

    @Test
    void adminRoutesRequireLoginAndProductWritesRequireCsrf() throws Exception {
        for (String path : new String[]{"/admin/products", "/admin/products/new", "/admin/products/1/edit"}) {
            assertThat(browser.get(path).statusCode()).isEqualTo(302);
        }
        assertThat(browser.postWithCsrf("/admin/login", "/admin/products", fields("blocked")).statusCode()).isEqualTo(302);
        assertThat(repository.existsBySlug(prefix + "-blocked")).isFalse();
        browser.login();
        assertThat(browser.get("/admin/products").statusCode()).isEqualTo(200);
        assertThat(browser.get("/admin/products/new").body()).contains("name=\"_csrf\"", "lang=\"fa\"", "dir=\"rtl\"");
        assertThat(browser.post("/admin/products", fields("blocked")).statusCode()).isEqualTo(403);
        Product saved = service.create(form("existing"));
        assertThat(browser.post("/admin/products/" + saved.getId() + "/active", Map.of("active", "false")).statusCode()).isEqualTo(403);
        assertThat(service.get(saved.getId()).isActive()).isTrue();
    }

    @Test
    void authenticatedAdminCanCreateEditAndActivateProductsThroughBoundForms() throws Exception {
        browser.login();
        var created = browser.postWithCsrf("/admin/products/new", "/admin/products", fields("http"));
        assertThat(created.statusCode()).isEqualTo(302);
        assertThat(browser.get("/admin/products").body()).contains("محصول ایجاد شد.");
        Product product = service.findAll().stream().filter(p -> p.getSlug().equals(prefix + "-http")).findFirst().orElseThrow();
        String path = "/admin/products/" + product.getId();
        assertThat(browser.get(path + "/edit").statusCode()).isEqualTo(200);
        var edit = new HashMap<>(fields("http"));
        edit.put("name", "محصول ویرایش‌شده"); edit.put("price", "3500000000"); edit.put("stockQuantity", "0");
        edit.put("featured", "true"); edit.put("displayOrder", "3");
        assertThat(browser.postWithCsrf(path + "/edit", path, edit).statusCode()).isEqualTo(302);
        assertThat(service.get(product.getId()).getPrice()).isEqualTo(3_500_000_000L);
        assertThat(service.get(product.getId()).getStockQuantity()).isZero();
        assertThat(service.get(product.getId()).isFeatured()).isTrue();
        assertThat(browser.postWithCsrf("/admin/products", path + "/active", Map.of("active", "false")).statusCode()).isEqualTo(302);
        assertThat(service.get(product.getId()).isActive()).isFalse();
        assertThat(browser.postWithCsrf("/admin/products", path + "/active", Map.of("active", "true")).statusCode()).isEqualTo(302);
        assertThat(service.get(product.getId()).isActive()).isTrue();
        assertThat(browser.get("/admin/products").body()).contains("محصول ویرایش‌شده", "3500000000", "ویژه");
        assertThat(browser.get("/admin/products/9223372036854775807/edit").statusCode()).isEqualTo(404);
    }

    @Test
    void adminShowsDuplicateAndNumericBindingErrorsWithoutSaving() throws Exception {
        browser.login();
        Product existing = service.create(form("existing"));
        var duplicate = new HashMap<>(fields("duplicate"));
        duplicate.put("slug", existing.getSlug());
        var response = browser.postWithCsrf("/admin/products/new", "/admin/products", duplicate);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("این نامک قبلاً استفاده شده است.", "id=\"slug-errors\"");
        duplicate.put("slug", prefix + "-duplicate"); duplicate.put("sku", existing.getSku());
        assertThat(browser.postWithCsrf("/admin/products/new", "/admin/products", duplicate).body()).contains("این کد کالا قبلاً استفاده شده است.");
        var invalid = new HashMap<>(fields("invalid"));
        invalid.put("price", "12.5"); invalid.put("stockQuantity", "-1"); invalid.put("type", "UNKNOWN");
        response = browser.postWithCsrf("/admin/products/new", "/admin/products", invalid);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("قیمت باید عدد صحیح", "موجودی نباید منفی باشد.", "نوع محصول معتبر انتخاب کنید.");
        invalid.put("price", "9223372036854775808");
        assertThat(browser.postWithCsrf("/admin/products/new", "/admin/products", invalid).body()).contains("قیمت باید عدد صحیح");
        assertThat(repository.existsBySlug(prefix + "-invalid")).isFalse();
    }

    private ProductForm form(String suffix) {
        ProductForm form = new ProductForm();
        form.setName("محصول آزمون"); form.setSlug(prefix + "-" + suffix); form.setSku(prefix + "-" + suffix);
        form.setPrice(125_000L); form.setStockQuantity(10); form.setCategoryId(category.getId());
        form.setType(ProductType.STATIONERY);
        return form;
    }

    private Map<String, String> fields(String suffix) {
        return Map.of("name", "محصول آزمون", "slug", prefix + "-" + suffix, "sku", prefix + "-" + suffix,
                "price", "125000", "stockQuantity", "10", "categoryId", category.getId().toString(),
                "type", "STATIONERY", "displayOrder", "0", "active", "true");
    }
}
