package com.noghterangi.order;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import com.noghterangi.category.*;
import com.noghterangi.product.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "store.payment.card-number=0000-0000-0000-0000", "store.payment.account-name=حساب آزمایشی",
        "store.payment.instructions=رسید آزمایشی را با شماره سفارش ارسال کنید."
})
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class CheckoutIntegrationTest {
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.validation.Validator validator;
    @LocalServerPort int port;
    private final String prefix = "checkout-" + UUID.randomUUID();
    private Category category;
    private Product product;
    private Product other;
    private TestBrowser browser;

    @BeforeEach void setup() {
        browser = new TestBrowser(port);
        var cf = new CategoryForm(); cf.setName("دسته سفارش"); cf.setSlug(prefix); category = categories.create(cf);
        product = createProduct(prefix, 25000L); other = createProduct(prefix + "-other", 10000L);
    }
    private Product createProduct(String slug, long price) {
        var pf = new ProductForm(); pf.setName("محصول سفارش"); pf.setSlug(slug); pf.setSku(slug);
        pf.setCategoryId(category.getId()); pf.setType(ProductType.STATIONERY); pf.setPrice(price); pf.setStockQuantity(7);
        return products.create(pf);
    }
    @AfterEach void cleanup() {
        browser.close();
        jdbc.update("delete from noghterangi.order_items where order_id in (select id from noghterangi.orders where customer_name = ?)", prefix);
        jdbc.update("delete from noghterangi.orders where customer_name = ?", prefix);
        productRepository.deleteAllById(List.of(product.getId(), other.getId())); categoryRepository.deleteById(category.getId());
    }
    private Map<String, String> fields() {
        return new HashMap<>(Map.of("customerName", prefix, "phone", "۰۹۱۲۳۴۵۶۷۸۹", "deliveryArea", "محله رنگی",
                "deliveryAddress", "خیابان نمونه، پلاک ۱۲", "customerNote", "<script>یادداشت</script>", "totalAmount", "1", "unitPrice", "1"));
    }
    private CheckoutForm form() {
        var form = new CheckoutForm(); form.setCustomerName(prefix); form.setPhone("09123456789");
        form.setDeliveryArea("محله رنگی"); form.setDeliveryAddress("نشانی نمونه"); return form;
    }
    private void add(Product item, int quantity) throws Exception {
        assertThat(browser.postWithCsrf("/products/" + item.getSlug(), "/cart/items",
                Map.of("productId", item.getId().toString(), "quantity", String.valueOf(quantity))).statusCode()).isEqualTo(302);
        assertThat(browser.getHtml("/cart").statusCode()).isEqualTo(200);
    }
    private HttpResponse<String> submit(Map<String, String> fields) throws Exception {
        return browser.postWithCsrf("/admin/login", "/checkout", fields);
    }
    private String location(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(302);
        return URI.create(response.headers().firstValue("location").orElseThrow()).getPath();
    }
    private long orderCount() { return jdbc.queryForObject("select count(*) from noghterangi.orders where customer_name = ?", Long.class, prefix); }
    private void retained() throws Exception {
        assertThat(browser.getHtml("/cart").body()).contains("aria-label=\"تعداد کل کالاها\">2</span>");
        assertThat(orderCount()).isZero();
    }

    @Test void emptyCheckoutRedirectsAndCsrfAndAdminProtectionRemain() throws Exception {
        assertThat(location(browser.getHtml("/checkout"))).isEqualTo("/cart");
        assertThat(browser.getHtml("/cart").body()).contains("سبد خرید خالی است");
        assertThat(browser.post("/checkout", fields()).statusCode()).isEqualTo(403);
        assertThat(location(submit(fields()))).isEqualTo("/cart");
        assertThat(browser.getHtml("/admin/products").statusCode()).isEqualTo(302);
        assertThat(orderCount()).isZero();
        assertThatThrownBy(() -> service.place(Map.of(), form())).isInstanceOf(CheckoutException.class);
    }
    @Test void requiredFieldsLengthsPhoneAndPersianDigitsAreValidated() throws Exception {
        add(product, 2);
        assertThat(browser.getHtml("/checkout").body()).contains("50,000", "name=\"_csrf\"");
        var invalid = fields(); invalid.put("customerName", " "); invalid.put("phone", "123");
        invalid.put("deliveryArea", ""); invalid.put("deliveryAddress", "");
        assertThat(location(submit(invalid))).isEqualTo("/checkout");
        String body = browser.getHtml("/checkout").body();
        assertThat(body).contains("نام و نام خانوادگی را وارد کنید", "شماره موبایل را", "محله یا محدوده تحویل", "نشانی تحویل را", "اطلاعات سفارش کامل نیست", "aria-invalid=\"true\"", "data-validation-summary", "aria-describedby=\"phone-help phone-error\"");
        retained();
        invalid = fields(); invalid.put("customerName", "ن".repeat(161)); invalid.put("deliveryArea", "م".repeat(161));
        invalid.put("deliveryAddress", "آ".repeat(1001)); invalid.put("customerNote", "ی".repeat(1001));
        assertThat(location(submit(invalid))).isEqualTo("/checkout");
        assertThat(browser.getHtml("/checkout").body()).contains("۱۶۰", "۱۰۰۰"); retained();
        var f = form(); f.setPhone("٠٩١٢٣٤٥٦٧٨٩"); assertThat(f.getPhone()).isEqualTo("09123456789");
        assertThat(validator.validate(f)).isEmpty();
    }
    @Test void anonymousCheckoutSnapshotsCurrentPricesAndNamesAndClearsOnlyAfterSuccess() throws Exception {
        add(product, 2); add(other, 3);
        var pf = ProductForm.from(products.get(product.getId())); pf.setPrice(40000L); products.update(product.getId(), pf);
        String path = location(submit(fields())); assertThat(path).startsWith("/orders/NR-");
        String number = path.substring("/orders/".length());
        Order order = orders.findByOrderNumber(number).orElseThrow();
        assertThat(order.getTotalAmount()).isEqualTo(110000L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_PAYMENT); assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getPhone()).isEqualTo("09123456789");
        assertThat(order.getItems()).hasSize(2);
        var line = order.getItems().stream().filter(i -> i.getProductId().equals(product.getId())).findFirst().orElseThrow();
        assertThat(line.getUnitPrice()).isEqualTo(40000L); assertThat(line.getQuantity()).isEqualTo(2); assertThat(line.getLineTotal()).isEqualTo(80000L);
        assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(5);
        assertThat(products.get(other.getId()).getStockQuantity()).isEqualTo(4);
        assertThat(browser.getHtml("/cart").body()).contains("سبد خریدت هنوز خالیه");
        var confirmation = browser.getHtml(path);
        assertThat(confirmation.statusCode()).isEqualTo(200);
        assertThat(confirmation.headers().firstValue("cache-control")).hasValue("no-store");
        assertThat(confirmation.body()).contains(number, "سفارش شما ثبت شد", "110,000", "در انتظار پرداخت", "0000-0000-0000-0000", "حساب آزمایشی", "رسید آزمایشی")
                .doesNotContain("<script>یادداشت", "customerName", "order_id");
        pf.setName("نام جدید"); pf.setPrice(999L); products.update(product.getId(), pf); products.setActive(product.getId(), false);
        productRepository.deleteById(product.getId());
        Order historical = orders.findByOrderNumber(number).orElseThrow();
        assertThat(historical.getItems().getFirst().getProductName()).isEqualTo("محصول سفارش");
        assertThat(historical.getTotalAmount()).isEqualTo(110000L);
        assertThat(browser.getHtml(path).body()).contains("40,000", "محصول سفارش").doesNotContain("نام جدید");
        assertThat(location(submit(fields()))).isEqualTo("/cart"); assertThat(orderCount()).isEqualTo(1);
        try (var stranger = new TestBrowser(port)) { assertThat(stranger.getHtml(path).statusCode()).isEqualTo(404); }
    }
    @Test void stockAndAvailabilityChangesRejectWithoutClearingCartOrCreatingOrders() throws Exception {
        add(product, 2);
        for (int stock : new int[]{1, 0}) {
            var pf = ProductForm.from(products.get(product.getId())); pf.setStockQuantity(stock); products.update(product.getId(), pf);
            assertThat(location(submit(fields()))).isEqualTo("/checkout");
            assertThat(location(browser.getHtml("/checkout"))).isEqualTo("/cart");
            assertThat(browser.getHtml("/cart").body()).contains("موجودی یکی از محصولات کافی نیست"); retained();
        }
        var pf = ProductForm.from(products.get(product.getId())); pf.setStockQuantity(7); products.update(product.getId(), pf);
        products.setActive(product.getId(), false); assertThat(location(submit(fields()))).isEqualTo("/checkout"); retained();
        products.setActive(product.getId(), true); categories.setActive(category.getId(), false);
        assertThat(location(submit(fields()))).isEqualTo("/checkout"); retained();
        categories.setActive(category.getId(), true); productRepository.deleteById(product.getId());
        assertThat(location(submit(fields()))).isEqualTo("/checkout"); retained();
    }
    @Test void laterInvalidItemRollsBackEarlierStockChanges() {
        var pf = ProductForm.from(other); pf.setStockQuantity(0); products.update(other.getId(), pf);
        assertThatThrownBy(() -> service.place(Map.of(product.getId(), 2, other.getId(), 1), form())).isInstanceOf(CheckoutException.class);
        assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(7); assertThat(orderCount()).isZero();
        assertThatThrownBy(() -> service.place(Map.of(product.getId(), 0), form())).isInstanceOf(CheckoutException.class);
        assertThatThrownBy(() -> service.place(Map.of(product.getId(), -1), form())).isInstanceOf(CheckoutException.class);
    }
    @Test void persistenceFailureRollsBackOrderItemsAndStockAndRetainsCart() throws Exception {
        add(product, 2);
        // Force an actual item insert failure after the order header was inserted.
        jdbc.execute("alter table noghterangi.order_items add constraint checkout_test_failure check (product_id <> " + product.getId() + ")");
        try {
            assertThat(location(submit(fields()))).isEqualTo("/checkout");
            assertThat(browser.getHtml("/checkout").body()).contains("ثبت سفارش انجام نشد").doesNotContain("PSQLException", "checkout_test_failure");
            retained(); assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(7);
            assertThat(jdbc.queryForObject("select count(*) from noghterangi.order_items where product_id = ?", Long.class, product.getId())).isZero();
        } finally { jdbc.execute("alter table noghterangi.order_items drop constraint checkout_test_failure"); }
    }
    @Test void concurrentCheckoutsCannotOversell() throws Exception {
        var pf = ProductForm.from(product); pf.setStockQuantity(3); products.update(product.getId(), pf);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> checkout = () -> {
                start.await();
                try { service.place(Map.of(product.getId(), 2), form()); return true; }
                catch (CheckoutException exception) { return false; }
            };
            var first = executor.submit(checkout); var second = executor.submit(checkout); start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(orderCount()).isEqualTo(1); assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(1);
    }
    @Test void simultaneousSubmissionsOfSameSessionCreateOnlyOneOrder() throws Exception {
        add(product, 2);
        var values = fields(); values.put("_csrf", TestBrowser.csrf(browser.getHtml("/checkout").body()));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> checkout = () -> { start.await(); return location(browser.post("/checkout", values)); };
            var first = executor.submit(checkout); var second = executor.submit(checkout); start.countDown();
            var paths = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(paths.stream().filter(p -> p.startsWith("/orders/")).count()).isEqualTo(1);
            assertThat(paths).contains("/cart");
        }
        assertThat(orderCount()).isEqualTo(1); assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(5);
    }
    @Test void numbersAreUniqueAndMoneyOverflowCannotPersistPartialOrder() {
        Order first = service.place(Map.of(product.getId(), 1), form());
        Order second = service.place(Map.of(product.getId(), 1), form());
        assertThat(first.getOrderNumber()).isNotEqualTo(second.getOrderNumber()).matches("NR-[0-9]{8}(-[A-F0-9]{4}){4}");
        var pf = ProductForm.from(products.get(product.getId())); pf.setPrice(Long.MAX_VALUE); products.update(product.getId(), pf);
        assertThatThrownBy(() -> service.place(Map.of(product.getId(), 2), form())).isInstanceOf(CheckoutException.class).hasMessageContaining("مبلغ");
        assertThatThrownBy(() -> service.place(Map.of(product.getId(), 1, other.getId(), 1), form())).isInstanceOf(CheckoutException.class);
        assertThat(orderCount()).isEqualTo(2); assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(5);
        assertThat(products.get(other.getId()).getStockQuantity()).isEqualTo(7);
    }
    @Test void missingPaymentSetupAndInvalidCustomerCannotCreateOrder() {
        var unconfigured = new OrderService(productRepository, orders, validator, new PaymentInstructions("", "", ""));
        assertThatThrownBy(() -> unconfigured.place(Map.of(product.getId(), 1), form())).isInstanceOf(CheckoutException.class).hasMessageContaining("اطلاعات پرداخت");
        assertThatThrownBy(() -> service.place(Map.of(product.getId(), 1), new CheckoutForm())).isInstanceOf(CheckoutException.class).hasMessageContaining("اطلاعات سفارش");
        assertThat(orderCount()).isZero(); assertThat(products.get(product.getId()).getStockQuantity()).isEqualTo(7);
    }
}
