package com.noghterangi.catalog;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class CatalogIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired MediaService media;
    @Autowired MediaStorageService storage;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private final String prefix = "catalog-" + UUID.randomUUID();
    private final List<Long> productIds = new ArrayList<>();
    private Category root, child, other;
    private TestBrowser browser;

    @BeforeEach void setUp() {
        browser = new TestBrowser(port);
        root = category("root", null); child = category("child", root.getId()); other = category("other", null);
    }
    @AfterEach void cleanUp() {
        browser.close();
        for (Long id : productIds) {
            media.images(id).forEach(image -> media.delete(id, image.getId()));
            productRepository.deleteById(id);
        }
        categoryRepository.deleteById(child.getId());
        categoryRepository.deleteById(root.getId());
        categoryRepository.deleteById(other.getId());
    }

    @Test void visibilityRequiresActiveProductAndItsOwnActiveCategoryButNotStock() {
        Product active = product("دفتر", 10, 1, root);
        Product unavailable = product("ناموجود", 20, 0, root);
        Product inactive = product("پنهان", 30, 2, root);
        products.setActive(inactive.getId(), false);
        Product hiddenCategory = product("دسته پنهان", 40, 2, other);
        categories.setActive(other.getId(), false);
        var result = catalog.search(new CatalogFilter(Map.of("q", prefix)));
        assertThat(result.getContent()).extracting(Product::getId).containsExactlyInAnyOrder(active.getId(), unavailable.getId())
                .doesNotContain(inactive.getId(), hiddenCategory.getId());
        assertThat(search(Map.of("available", "true")).getContent()).extracting(Product::getId).containsExactly(active.getId());
    }

    @Test void parentIncludesDirectActiveChildrenAndChildFilterIsExact() {
        Product direct = product("دفتر", 10, 1, root);
        Product nested = product("کتاب", 20, 1, child);
        product("دیگر", 30, 1, other);
        assertThat(search(Map.of()).getContent()).extracting(Product::getId).containsExactlyInAnyOrder(direct.getId(), nested.getId());
        assertThat(search(Map.of("category", child.getSlug())).getContent()).extracting(Product::getId).containsExactly(nested.getId());
        categories.setActive(child.getId(), false);
        assertThat(search(Map.of()).getContent()).extracting(Product::getId).containsExactly(direct.getId());
        var unknown = new CatalogFilter(Map.of("category", prefix + "-missing"));
        assertThat(catalog.search(unknown)).isEmpty(); assertThat(unknown.getMessages()).contains("دسته‌بندی فعال پیدا نشد. دسته دیگری انتخاب کنید.");
        assertThat(search(Map.of("category", child.getSlug()))).isEmpty();
    }

    @Test void minimumMaximumAndCombinedPriceRangesAreInclusiveLongValues() {
        product("رایگان", 0, 1, root); product("ارزان", 10, 1, root); product("متوسط", 20, 1, root);
        product("گران", 3_500_000_000L, 1, root); product("بیشینه", Long.MAX_VALUE, 1, root);
        assertThat(search(Map.of("minPrice", "20", "sort", "LOWEST_PRICE")).getContent()).extracting(Product::getPrice)
                .containsExactly(20L, 3_500_000_000L, Long.MAX_VALUE);
        assertThat(search(Map.of("maxPrice", "10", "sort", "LOWEST_PRICE")).getContent()).extracting(Product::getPrice).containsExactly(0L, 10L);
        assertThat(search(Map.of("minPrice", "10", "maxPrice", "20", "sort", "LOWEST_PRICE")).getContent()).extracting(Product::getPrice).containsExactly(10L, 20L);
        assertThat(search(Map.of("minPrice", "20", "maxPrice", "20")).getContent()).extracting(Product::getPrice).containsExactly(20L);
    }

    @Test void exactlyThreeSortsUseDatabaseOrderWithStableIdTies() {
        Product first = product("یک", 20, 1, root);
        Product second = product("دو", 10, 1, root);
        Product third = product("سه", 20, 1, root);
        jdbc.update("update noghterangi.product set created_at = timestamp with time zone '2020-01-01 00:00:00+00' where id in (?, ?, ?)", first.getId(), second.getId(), third.getId());
        jdbc.update("update noghterangi.product set created_at = timestamp with time zone '2021-01-01 00:00:00+00' where id = ?", first.getId());
        assertThat(CatalogSort.values()).hasSize(3);
        assertThat(search(Map.of("sort", "NEWEST")).getContent()).extracting(Product::getId).containsExactly(first.getId(), third.getId(), second.getId());
        assertThat(search(Map.of("sort", "LOWEST_PRICE")).getContent()).extracting(Product::getId).containsExactly(second.getId(), third.getId(), first.getId());
        assertThat(search(Map.of("sort", "HIGHEST_PRICE")).getContent()).extracting(Product::getId).containsExactly(third.getId(), first.getId(), second.getId());
    }

    @Test void springDataPaginationBoundsResultsAndHandlesPastLastPage() {
        for (int i = 0; i < 25; i++) { product("دفتر " + i, i, 1, root); }
        var first = search(Map.of("sort", "LOWEST_PRICE"));
        var second = search(Map.of("sort", "LOWEST_PRICE", "page", "1"));
        var third = search(Map.of("sort", "LOWEST_PRICE", "page", "2"));
        assertThat(first.getTotalElements()).isEqualTo(25); assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.getContent()).hasSize(12); assertThat(second.getContent()).hasSize(12); assertThat(third.getContent()).hasSize(1);
        assertThat(second.getContent()).extracting(Product::getPrice).containsExactly(12L,13L,14L,15L,16L,17L,18L,19L,20L,21L,22L,23L);
        assertThat(search(Map.of("page", "3"))).isEmpty();
    }

    @Test void persianCharacterVariantsWhitespaceAndUpdatesMatchInPostgres() {
        Product arabic = product("كيف\u200c\u200cرنگي", 10, 2, root);
        Product persian = product("کیف\t  رنگی", 20, 3, root);
        assertThat(search(Map.of("q", " كيف\u00a0 رنگی ")).getContent()).extracting(Product::getId).containsExactlyInAnyOrder(arabic.getId(), persian.getId());
        String variants = " \tك\u0085\u00a0\u1680\u2000\u2007\u200b\u200c\u200d\u2028\u2029\u202f\u205f\u3000\ufeffي\n ";
        assertThat(PersianTextNormalizer.normalize(variants)).isEqualTo("ک ی");
        assertThat(jdbc.queryForObject("select noghterangi.normalize_persian(?)", String.class, variants)).isEqualTo(PersianTextNormalizer.normalize(variants));
        ProductForm edit = ProductForm.from(products.get(arabic.getId())); edit.setName(prefix + " دفتر تازه"); products.update(arabic.getId(), edit);
        assertThat(search(Map.of("q", "کیف رنگی")).getContent()).extracting(Product::getId).containsExactly(persian.getId());
        assertThat(search(Map.of("q", "دفتر تازه")).getContent()).extracting(Product::getId).containsExactly(arabic.getId());
    }

    @Test void searchIsLiteralAndCombinedFiltersStayInTheDatabase() {
        Product matching = product("كيف رنگي", 50, 1, child);
        product("کیف رنگی", 100, 1, root); product("کیف رنگی", 50, 0, root); product("دفتر", 50, 1, root);
        Product literal = product("تخفیف %_!", 5, 1, root);
        assertThat(search(Map.of("q", "کیف رنگی", "minPrice", "40", "maxPrice", "60", "available", "true", "sort", "HIGHEST_PRICE")).getContent())
                .extracting(Product::getId).containsExactly(matching.getId());
        assertThat(search(Map.of("q", "%_!")).getContent()).extracting(Product::getId).containsExactly(literal.getId());
        assertThat(search(Map.of("q", "' OR 1=1 --"))).isEmpty();
    }

    @Test void invalidUrlValuesAreFriendlyAndNeverExposeExceptions() throws Exception {
        product("دفتر", 20, 1, root);
        for (String price : List.of("-1", "1.5", "oops", "9223372036854775808")) {
            var response = browser.get("/products?category=" + root.getSlug() + "&minPrice=" + price);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("قیمت باید عدد صحیح", "catalog-empty").doesNotContain("data-catalog-product=", "Exception");
        }
        assertThat(browser.get("/products?minPrice=30&maxPrice=20").body()).contains("حداقل قیمت نباید بیشتر");
        for (String page : List.of("-1", "oops", "9999999999999999999", "2147483647")) {
            assertThat(browser.get("/products?category=" + root.getSlug() + "&page=" + page + "&sort=name,asc").body())
                    .contains("شماره صفحه نامعتبر", "ترتیب نامعتبر", "data-catalog-product=");
        }
        assertThat(browser.get("/products?category=does-not-exist").body()).contains("دسته‌بندی فعال پیدا نشد", "catalog-empty");
        assertThat(browser.get("/products?category=" + root.getSlug() + "&page=99").body()).contains("صفحه اول همین جست‌وجو", "catalog-empty");
    }

    @Test void anonymousHtmlPreservesSearchFilterStateAndPaginationAndEscapesNames() throws Exception {
        for (int i = 0; i < 13; i++) { product("مداد + رنگی & شادی <script> " + i, 50, 1, root); }
        String q = URLEncoder.encode("مداد + رنگی & شادی", StandardCharsets.UTF_8);
        var response = browser.get("/products?q=" + q + "&category=" + root.getSlug() + "&minPrice=40&maxPrice=60&available=true&sort=LOWEST_PRICE");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("dir=\"rtl\"", "value=\"مداد + رنگی &amp; شادی\"", "value=\"40\"", "value=\"60\"", "&lt;script&gt;", "دسته root / دسته child", "rel=\"next\"", "page=1", "available=true", "sort=LOWEST_PRICE")
                .doesNotContain("<script> 0", "th:each");
        assertThat(response.body().split("data-catalog-product=", -1)).hasSize(13);
        var next = java.util.regex.Pattern.compile("<a(?=[^>]*rel=\"next\")(?=[^>]*href=\"([^\"]+)\")[^>]*>").matcher(response.body());
        assertThat(next.find()).isTrue();
        String nextUrl = next.group(1).replace("&amp;", "&");
        assertThat(nextUrl).contains("%2B", "%26");
        var nextPage = browser.get(nextUrl);
        assertThat(nextPage.statusCode()).isEqualTo(200);
        assertThat(nextPage.body().split("data-catalog-product=", -1)).hasSize(2);
        var fresh = browser.get("/products?category=" + root.getSlug());
        assertThat(fresh.body()).contains("value=\"\"", "NEWEST");
        assertThat(browser.get("/").body()).contains("/products?category=" + root.getSlug(), "action=\"/products\"");
    }

    @Test void primaryImagePlaceholderFeaturedAndUnavailableCardsUseExistingMedia() throws Exception {
        Product pictured = product("تصویردار", 10, 1, root);
        Product plain = product("بدون تصویر", 20, 0, root);
        ProductForm edit = ProductForm.from(pictured); edit.setFeatured(true); products.update(pictured.getId(), edit);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var file = new MockMultipartFile("files", "test.png", "image/png", bytes.toByteArray());
        media.upload(pictured.getId(), List.of(file,file));
        var images = media.images(pictured.getId()); media.reorder(pictured.getId(), images.getFirst().getId(), 8);
        String body = browser.get("/products?category=" + root.getSlug()).body();
        assertThat(body).contains("/media/" + images.getLast().getId(), "بدون تصویر", "ناموجود", "ویژه", "data-catalog-image")
                .doesNotContain("/media/" + images.getFirst().getId())
                .contains("/products/" + pictured.getSlug());
        assertThat(browser.get("/media/" + images.getLast().getId()).statusCode()).isEqualTo(200);
        storage.delete(images.getLast().getStoredFileName());
        assertThat(browser.get("/media/" + images.getLast().getId()).statusCode()).isEqualTo(404);
        assertThat(browser.get("/products?category=" + root.getSlug()).statusCode()).isEqualTo(200);
        assertThat(browser.get("/js/catalog-images.js").statusCode()).isEqualTo(200);
    }

    private org.springframework.data.domain.Page<Product> search(Map<String, String> values) {
        var parameters = new HashMap<>(values); parameters.putIfAbsent("category", root.getSlug());
        return catalog.search(new CatalogFilter(parameters));
    }
    private Category category(String suffix, Long parentId) {
        var form = new CategoryForm(); form.setName("دسته " + suffix); form.setSlug(prefix + "-" + suffix); form.setParentId(parentId);
        return categories.create(form);
    }
    private Product product(String name, long price, int stock, Category category) {
        var form = new ProductForm(); form.setName(prefix + " " + name);
        form.setSlug(prefix + "-" + productIds.size()); form.setSku(prefix + "-" + productIds.size());
        form.setCategoryId(category.getId()); form.setType(ProductType.STATIONERY); form.setPrice(price); form.setStockQuantity(stock);
        var product = products.create(form); productIds.add(product.getId()); return product;
    }
}
