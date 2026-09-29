package com.noghterangi.product;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import com.noghterangi.category.*;
import com.noghterangi.media.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class ProductDetailIntegrationTest {
    @Autowired ProductService products;
    @Autowired ProductRepository repository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired MediaService media;
    @Autowired MediaStorageService storage;
    @LocalServerPort int port;
    private final String prefix = "detail-" + UUID.randomUUID();
    private Category category;
    private Product product;
    private TestBrowser browser;

    @BeforeEach void setUp() {
        browser = new TestBrowser(port);
        var categoryForm = new CategoryForm(); categoryForm.setName("دسته جزئیات"); categoryForm.setSlug(prefix);
        category = categories.create(categoryForm);
        var form = new ProductForm(); form.setName("محصول جزئیات"); form.setSlug(prefix); form.setSku(prefix);
        form.setCategoryId(category.getId()); form.setType(ProductType.STATIONERY); form.setPrice(185000L); form.setStockQuantity(7);
        product = products.create(form);
    }
    @AfterEach void cleanup() {
        browser.close();
        media.images(product.getId()).forEach(image -> media.delete(product.getId(), image.getId()));
        repository.deleteById(product.getId()); categoryRepository.deleteById(category.getId());
    }
    private String path() { return "/products/" + product.getSlug(); }
    private ProductForm edit() { return ProductForm.from(products.get(product.getId())); }

    @Test void anonymousDetailBySlugHasPriceCategoryBreadcrumbTitleAndStockBoundQuantity() throws Exception {
        var form = edit(); form.setBrand("برند رنگی"); form.setShortDescription("توضیح کوتاه محصول"); products.update(product.getId(), form);
        var response = browser.getHtml(path());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<title>محصول جزئیات | نقطه رنگی</title>", "185,000", "تومان", "برند رنگی", "توضیح کوتاه محصول",
                "/products?category=" + category.getSlug(), "مسیر صفحه", "موجودی: 7 عدد", "dir=\"rtl\"");
        String input = tag(response.body(), "input", "quantity");
        assertThat(input).contains("min=\"1\"", "max=\"7\"", "step=\"1\"", "value=\"1\"").doesNotContain("disabled");
        assertThat(tag(response.body(), "button", "add-to-cart")).contains("disabled", "type=\"button\"");
        assertThat(response.body()).contains("در حال حاضر امکان افزودن به سبد خرید وجود ندارد").doesNotContain("action=\"/cart", "th:");
        assertThat(browser.getHtml("/products?category=" + category.getSlug()).body()).contains("href=\"" + path() + "\"");
    }

    @Test void unknownInactiveAndInactiveCategoryReturnFriendly404WithoutProductData() throws Exception {
        assertNotFound(browser.getHtml("/products/" + prefix + "-unknown"));
        products.setActive(product.getId(), false); assertNotFound(browser.getHtml(path()));
        products.setActive(product.getId(), true); categories.setActive(category.getId(), false); assertNotFound(browser.getHtml(path()));
        categories.setActive(category.getId(), true); assertThat(browser.getHtml(path()).statusCode()).isEqualTo(200);
    }

    @Test void unavailableDisablesQuantityAndCartButRemainsViewable() throws Exception {
        var form = edit(); form.setStockQuantity(0); products.update(product.getId(), form);
        var response = browser.getHtml(path());
        assertThat(response.statusCode()).isEqualTo(200); assertThat(response.body()).contains("ناموجود");
        assertThat(tag(response.body(), "input", "quantity")).contains("disabled", "min=\"1\"", "max=\"1\"");
        assertThat(tag(response.body(), "button", "add-to-cart")).contains("disabled");
        assertThat(response.body()).doesNotContain("موجودی: 0", "action=\"/cart");
    }

    @Test void optionalTextIsOmittedAndDescriptionIsEscapedPlainText() throws Exception {
        String initial = browser.getHtml(path()).body();
        assertThat(initial).doesNotContain("برند:", "id=\"description-title\"", "id=\"game-specifications\"", "null");
        var form = edit(); form.setDescription("خط اول\n<script>alert('x')</script>\nخط سوم");
        form.setShortDescription("<b>متن کوتاه</b>"); products.update(product.getId(), form);
        String body = browser.getHtml(path()).body();
        assertThat(body).contains("خط اول\n&lt;script&gt;", "&lt;b&gt;متن کوتاه&lt;/b&gt;", "product-description")
                .doesNotContain("<script>alert", "<b>متن کوتاه</b>");
    }

    @Test void gameSpecificationsShowOnlyPopulatedFieldsAndDisappearForOtherTypes() throws Exception {
        var form = edit(); form.setType(ProductType.BOARD_GAME); form.setMinimumAge(8); form.setMaximumPlayers(6); form.setPlayingTimeMinutes(30);
        products.update(product.getId(), form);
        String body = browser.getHtml(path()).body();
        assertThat(body).contains("id=\"game-specifications\"", "حداقل سن", "8 سال", "حداکثر بازیکنان", "6 نفر", "30 دقیقه")
                .doesNotContain("حداکثر سن", "حداقل بازیکنان");
        form.setMinimumAge(null); form.setMaximumPlayers(null); form.setPlayingTimeMinutes(null); products.update(product.getId(), form);
        assertThat(browser.getHtml(path()).body()).doesNotContain("id=\"game-specifications\"");
        form.setType(ProductType.BOOK); products.update(product.getId(), form);
        assertThat(browser.getHtml(path()).body()).doesNotContain("id=\"game-specifications\"");
    }

    @Test void orderedImagesUseFirstAsPrimaryAndMissingFilesDoNotBreakDetail() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3,2,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var file = new MockMultipartFile("files", "test.png", "image/png", bytes.toByteArray());
        media.upload(product.getId(), List.of(file,file));
        var images = media.images(product.getId()); media.reorder(product.getId(), images.getFirst().getId(), 5);
        String body = browser.getHtml(path()).body();
        assertThat(tag(body, "img", "primary-image")).contains("src=\"/media/" + images.getLast().getId() + "\"", "alt=\"محصول جزئیات");
        assertThat(body).contains("href=\"/media/" + images.getFirst().getId(), "data-gallery-thumbnail", "نمایش تصویر 2");
        storage.delete(images.getLast().getStoredFileName());
        assertThat(browser.get("/media/" + images.getLast().getId()).statusCode()).isEqualTo(404);
        assertThat(browser.getHtml(path()).statusCode()).isEqualTo(200);
        assertThat(browser.getHtml(path()).body()).contains("بدون تصویر", "/js/product-gallery.js");
    }

    @Test void productWithoutImagesShowsCleanPlaceholderWithoutGalleryControls() throws Exception {
        String body = browser.getHtml(path()).body();
        assertThat(body).contains("بدون تصویر").doesNotContain("id=\"primary-image\"", "data-gallery-thumbnail");
    }

    private void assertNotFound(java.net.http.HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("صفحه پیدا نشد", "مشاهده محصولات").doesNotContain("محصول جزئیات", "185,000", "Exception", "org.hibernate");
    }
    private String tag(String html, String element, String id) {
        var matcher = Pattern.compile("<" + element + "(?=[^>]*id=\"" + id + "\")[^>]*>").matcher(html);
        assertThat(matcher.find()).as(id).isTrue(); return matcher.group();
    }
}
