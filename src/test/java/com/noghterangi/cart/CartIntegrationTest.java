package com.noghterangi.cart;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import com.noghterangi.category.*;
import com.noghterangi.product.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class CartIntegrationTest {
    @Autowired ProductService products;
    @Autowired ProductRepository repository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CartService carts;
    @LocalServerPort int port;
    private Category category;
    private Product product;
    private TestBrowser browser;

    @BeforeEach void setup() {
        browser = new TestBrowser(port);
        String prefix = "cart-" + UUID.randomUUID();
        var cf = new CategoryForm(); cf.setName("دسته سبد"); cf.setSlug(prefix);
        category = categories.create(cf);
        var pf = new ProductForm(); pf.setName("محصول سبد"); pf.setSlug(prefix); pf.setSku(prefix);
        pf.setCategoryId(category.getId()); pf.setType(ProductType.STATIONERY); pf.setPrice(25000L); pf.setStockQuantity(7);
        product = products.create(pf);
    }
    @AfterEach void cleanup() {
        browser.close(); repository.deleteById(product.getId()); categoryRepository.deleteById(category.getId());
    }
    private void add(String quantity) throws Exception {
        var response = browser.postWithCsrf("/admin/login", "/cart/items",
                Map.of("productId", product.getId().toString(), "quantity", quantity, "price", "1"));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location")).hasValueSatisfying(
                location -> assertThat(java.net.URI.create(location).getPath()).isEqualTo("/cart"));
    }
    private void action(String action, String quantity) throws Exception {
        assertThat(browser.postWithCsrf("/cart", "/cart/items/" + product.getId(),
                Map.of("action", action, "quantity", quantity)).statusCode()).isEqualTo(302);
    }
    private String page() throws Exception { return browser.getHtml("/cart").body(); }
    private void count(int quantity) throws Exception {
        assertThat(page()).contains("aria-label=\"تعداد کل کالاها\">" + quantity + "</span>");
    }

    @Test void emptyCartAndIndependentAnonymousSessions() throws Exception {
        assertThat(browser.getHtml("/cart").statusCode()).isEqualTo(200);
        assertThat(page()).contains("سبد خریدت هنوز خالیه", "href=\"/products\""); count(0);
        add("2"); count(2);
        try (var other = new TestBrowser(port)) { assertThat(other.getHtml("/cart").body()).contains("سبد خریدت هنوز خالیه"); }
    }
    @Test void addMergeIncreaseDecreaseUpdateRemoveAndClearUsePrg() throws Exception {
        add("2"); count(2); assertThat(page()).contains("50,000", "25,000");
        add("1"); count(3);
        action("increase", "1"); count(4);
        action("decrease", "1"); count(3);
        action("update", "6"); count(6); assertThat(page()).contains("150,000");
        assertThat(browser.getHtml("/products").body()).contains("aria-label=\"تعداد کل کالاها\">6</span>");
        action("remove", "1"); count(0);
        add("1");
        assertThat(browser.postWithCsrf("/cart", "/cart/clear", Map.of()).statusCode()).isEqualTo(302);
        assertThat(page()).contains("سبد خرید خالی شد", "سبد خریدت هنوز خالیه"); count(0);
    }
    @Test void invalidAndMalformedQuantitiesNeverChangeState() throws Exception {
        add("2"); count(2);
        for (String invalid : new String[]{"0", "-1", "6", "2147483648", "1.5", "abc", ""}) {
            add(invalid); assertThat(page()).as("invalid quantity: %s", invalid).contains("role=\"alert\""); count(2);
        }
        for (String invalid : new String[]{"0", "-2", "8", "2147483647"}) {
            action("update", invalid); assertThat(page()).contains("role=\"alert\""); count(2);
        }
        action("update", "7"); action("increase", "1"); assertThat(page()).contains("بیشتر از موجودی"); count(7);
        action("update", "1"); action("decrease", "1"); assertThat(page()).contains("حداقل ۱"); count(1);
    }
    @Test void priceStockAndAvailabilityAreReloadedWithoutRemovingItems() throws Exception {
        add("4");
        var form = ProductForm.from(products.get(product.getId())); form.setPrice(40000L); form.setStockQuantity(2);
        products.update(product.getId(), form);
        assertThat(page()).contains("40,000", "160,000", "بیشتر از موجودی"); count(4);
        action("increase", "1"); count(4);
        action("update", "2"); count(2); assertThat(page()).contains("80,000").doesNotContain("سبد نیاز به اصلاح دارد");
        form.setStockQuantity(0); products.update(product.getId(), form);
        assertThat(page()).contains("این محصول ناموجود است"); add("1"); count(2);
        form.setStockQuantity(7); products.update(product.getId(), form); products.setActive(product.getId(), false);
        assertThat(page()).contains("قابل خرید نیست"); action("increase", "1"); count(2);
        products.setActive(product.getId(), true); categories.setActive(category.getId(), false);
        assertThat(page()).contains("قابل خرید نیست"); add("1"); count(2);
        action("remove", "1"); count(0);
    }
    @Test void missingProductRemainsVisibleAndRemovable() throws Exception {
        add("1"); repository.deleteById(product.getId());
        assertThat(page()).contains("محصول حذف‌شده", "دیگر در دسترس نیست");
        action("remove", "1"); count(0);
        assertThat(browser.postWithCsrf("/admin/login", "/cart/items", Map.of("productId", product.getId().toString(), "quantity", "1")).statusCode()).isEqualTo(302);
        assertThat(page()).contains("محصول پیدا نشد");
    }
    @Test void csrfRequiredForEveryMutationAndAdminStillProtected() throws Exception {
        for (String path : new String[]{"/cart/items", "/cart/items/" + product.getId(), "/cart/clear"}) {
            assertThat(browser.post(path, Map.of("productId", product.getId().toString(), "quantity", "1")).statusCode()).isEqualTo(403);
        }
        for (String path : new String[]{"/admin", "/admin/categories", "/admin/products"}) {
            assertThat(browser.getHtml(path).statusCode()).isEqualTo(302);
        }
        add("1"); assertThat(page()).contains("سبد خرید به‌روز شد"); count(1);
    }
    @Test void totalsAcrossProductsAndOverflowAreSafe() {
        var cart = new Cart(); carts.change(cart, product.getId(), 2, "add");
        var form = ProductForm.from(product); form.setSlug(form.getSlug() + "-other"); form.setSku(form.getSku() + "-other"); form.setPrice(10000L);
        Product other = products.create(form);
        try {
            carts.change(cart, other.getId(), 3, "add");
            assertThat(cart.getCount()).isEqualTo(5); assertThat(carts.resolve(cart).subtotal()).isEqualTo(80000L);
        } finally { cart.remove(other.getId()); repository.deleteById(other.getId()); }
        var overflow = ProductForm.from(products.get(product.getId())); overflow.setPrice(Long.MAX_VALUE);
        products.update(product.getId(), overflow);
        assertThat(carts.resolve(cart).subtotal()).isNull(); assertThat(carts.resolve(cart).valid()).isFalse();
        assertThat(carts.resolve(cart).lines().getFirst().warning()).contains("بیش از حد مجاز");
    }
}
