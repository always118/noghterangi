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
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "store.payment.card-number=0000-0000-0000-0000", "store.payment.account-name=حساب آزمایشی",
        "store.payment.instructions=رسید آزمایشی را با شماره سفارش ارسال کنید."
})
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class AdminOrderIntegrationTest {
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private final String prefix = "admin-order-" + UUID.randomUUID();
    private Category category;
    private Product product;
    private Product other;
    private TestBrowser browser;

    @BeforeEach void setup() {
        browser = new TestBrowser(port);
        var cf = new CategoryForm(); cf.setName("دسته سفارش مدیریت"); cf.setSlug(prefix); category = categories.create(cf);
        product = createProduct(prefix, 25000L); other = createProduct(prefix + "-other", 10000L);
    }
    private Product createProduct(String slug, long price) {
        var pf = new ProductForm(); pf.setName("نام تاریخی محصول"); pf.setSlug(slug); pf.setSku(slug);
        pf.setCategoryId(category.getId()); pf.setType(ProductType.STATIONERY); pf.setPrice(price); pf.setStockQuantity(1000);
        return products.create(pf);
    }
    private CheckoutForm form() {
        var form = new CheckoutForm(); form.setCustomerName(prefix); form.setPhone("09123456789");
        form.setDeliveryArea("محله نمونه"); form.setDeliveryAddress("نشانی آزمایشی\n<script>آدرس</script>"); form.setCustomerNote("<b>یادداشت</b>");
        return form;
    }
    private Order place(int quantity) { return service.place(Map.of(product.getId(), quantity), form()); }
    private int stock(Product item) { return products.get(item.getId()).getStockQuantity(); }
    private OrderStatus status(Order order) { return service.get(order.getId()).getStatus(); }
    private String path(Order order) { return "/admin/orders/" + order.getId(); }
    private void post(Order order, String next) throws Exception {
        var response = browser.postWithCsrf("/admin/orders", path(order) + "/status", Map.of("status", next));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(URI.create(response.headers().firstValue("location").orElseThrow()).getPath()).isEqualTo(path(order));
    }
    @AfterEach void cleanup() {
        browser.close();
        jdbc.update("delete from noghterangi.order_items where order_id in (select id from noghterangi.orders where customer_name = ?)", prefix);
        jdbc.update("delete from noghterangi.orders where customer_name = ?", prefix);
        productRepository.deleteAllById(List.of(product.getId(), other.getId())); categoryRepository.deleteById(category.getId());
    }

    @Test void adminAccessCsrfMissingOrderAndMalformedInputAreHandled() throws Exception {
        Order order = place(2);
        for (String url : List.of("/admin/orders", path(order))) {
            var response = browser.getHtml(url); assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.body()).doesNotContain(prefix, "09123456789");
        }
        assertThat(browser.postWithCsrf("/admin/login", path(order) + "/status", Map.of("status", "CANCELLED")).statusCode()).isEqualTo(302);
        assertThat(status(order)).isEqualTo(OrderStatus.WAITING_FOR_PAYMENT); assertThat(stock(product)).isEqualTo(998);
        browser.login();
        assertThat(browser.post(path(order) + "/status", Map.of("status", "CANCELLED")).statusCode()).isEqualTo(403);
        assertThat(browser.getHtml("/admin/orders/9223372036854775807").statusCode()).isEqualTo(302);
        assertThat(browser.getHtml("/admin/orders").body()).contains("سفارش پیدا نشد");
        assertThat(browser.postWithCsrf("/admin/orders", "/admin/orders/9223372036854775807/status", Map.of("status", "PAID")).statusCode()).isEqualTo(302);
        assertThat(browser.getHtml("/admin/orders").body()).contains("سفارش پیدا نشد");
        post(order, "HACKED"); assertThat(browser.getHtml(path(order)).body()).contains("وضعیت انتخاب‌شده معتبر نیست");
        for (String query : List.of("?status=HACKED", "?page=-1", "?page=bad")) {
            assertThat(browser.getHtml("/admin/orders" + query).body()).contains("وضعیت یا شماره صفحه معتبر نیست").doesNotContain("data-order-id");
        }
        assertThat(stock(product)).isEqualTo(998);
    }
    @Test void listUsesNewestFirstDatabasePaginationAndStatusFilter() throws Exception {
        List<Order> created = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            Order order = place(1); created.add(order);
            jdbc.update("update noghterangi.orders set created_at = ? where id = ?", java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i)), order.getId());
        }
        var firstPage = service.list(null, 0); var secondPage = service.list(null, 1);
        assertThat(firstPage.getTotalElements()).isEqualTo(23); assertThat(firstPage.getContent()).hasSize(20);
        assertThat(firstPage.getContent().getFirst().getId()).isEqualTo(created.getLast().getId());
        assertThat(secondPage.getContent()).hasSize(3); assertThat(secondPage.getContent().getLast().getId()).isEqualTo(created.getFirst().getId());
        service.changeStatus(created.getLast().getId(), OrderStatus.PAID);
        browser.login();
        var list = browser.getHtml("/admin/orders"); assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.headers().firstValue("cache-control")).hasValue("no-store");
        assertThat(list.body()).contains(prefix, "09123456789", "25,000", "بعدی", "زمان ثبت");
        assertThat(list.body().indexOf(created.getLast().getOrderNumber())).isLessThan(list.body().indexOf(created.get(21).getOrderNumber()));
        String filtered = browser.getHtml("/admin/orders?status=PAID").body();
        assertThat(filtered).contains(created.getLast().getOrderNumber()).doesNotContain(created.getFirst().getOrderNumber());
        String waiting = browser.getHtml("/admin/orders?status=WAITING_FOR_PAYMENT").body();
        assertThat(waiting).contains("status=WAITING_FOR_PAYMENT&amp;page=1");
        assertThat(browser.getHtml("/admin/orders?status=WAITING_FOR_PAYMENT&page=1").body()).contains(created.getFirst().getOrderNumber());
        assertThat(browser.getHtml("/admin/orders?status=DELIVERED").body()).contains("سفارشی برای نمایش پیدا نشد");
    }
    @Test void detailUsesHistoricalSnapshotsAndEscapesCustomerFields() throws Exception {
        Order order = place(3);
        var pf = ProductForm.from(products.get(product.getId())); pf.setName("نام فعلی متفاوت"); pf.setPrice(999L); products.update(product.getId(), pf);
        products.setActive(product.getId(), false); productRepository.deleteById(product.getId());
        browser.login(); var response = browser.getHtml(path(order)); assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("نام تاریخی محصول", "25,000", "75,000", "محله نمونه", "09123456789", "&lt;script&gt;", "&lt;b&gt;")
                .doesNotContain("نام فعلی متفاوت", "<script>آدرس", "<b>یادداشت");
        try (var publicBrowser = new TestBrowser(port)) {
            assertThat(publicBrowser.getHtml("/orders/" + order.getOrderNumber()).statusCode()).isEqualTo(404);
        }
    }
    @Test void forwardFlowDoesNotChangeStockAndDeliveredIsTerminal() throws Exception {
        Order order = place(4); browser.login();
        for (OrderStatus next : List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.DELIVERED)) {
            post(order, next.name()); assertThat(browser.getHtml(path(order)).body()).contains("وضعیت سفارش به‌روز شد", next.getLabel());
            assertThat(status(order)).isEqualTo(next); assertThat(stock(product)).isEqualTo(996);
        }
        for (OrderStatus next : List.of(OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.WAITING_FOR_PAYMENT)) {
            assertThatThrownBy(() -> service.changeStatus(order.getId(), next)).isInstanceOf(OrderStatusException.class);
        }
        assertThat(status(order)).isEqualTo(OrderStatus.DELIVERED); assertThat(stock(product)).isEqualTo(996);
        assertThat(browser.getHtml(path(order)).body()).contains("وضعیت نهایی").doesNotContain("id=\"next-status\"");
    }
    @Test void invalidTransitionsAndRepeatedCancellationCannotChangeInventory() throws Exception {
        Order order = service.place(Map.of(product.getId(), 3, other.getId(), 5), form()); browser.login();
        post(order, "READY"); assertThat(browser.getHtml(path(order)).body()).contains("این تغییر وضعیت مجاز نیست");
        assertThat(stock(product)).isEqualTo(997); assertThat(stock(other)).isEqualTo(995);
        products.setActive(other.getId(), false);
        post(order, "CANCELLED"); assertThat(browser.getHtml(path(order)).body()).contains("سفارش لغو شد و موجودی کالاها بازگردانده شد");
        assertThat(stock(product)).isEqualTo(1000); assertThat(stock(other)).isEqualTo(1000);
        post(order, "CANCELLED"); assertThat(browser.getHtml(path(order)).body()).contains("قبلاً لغو شده");
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.PAID)).isInstanceOf(OrderStatusException.class);
        assertThat(status(order)).isEqualTo(OrderStatus.CANCELLED); assertThat(stock(product)).isEqualTo(1000); assertThat(stock(other)).isEqualTo(1000);
        assertThat(service.get(order.getId()).getItems()).allSatisfy(i -> assertThat(i.getProductName()).isEqualTo("نام تاریخی محصول"));
    }
    @Test void cancellationIsAllowedFromEveryPreDeliveryState() {
        for (int advance = 0; advance < 4; advance++) {
            Order order = place(2);
            for (OrderStatus next : List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY).subList(0, advance)) {
                service.changeStatus(order.getId(), next);
            }
            service.changeStatus(order.getId(), OrderStatus.CANCELLED);
            assertThat(stock(product)).isEqualTo(1000); assertThat(status(order)).isEqualTo(OrderStatus.CANCELLED);
        }
    }
    @Test void concurrentCancellationRestoresStockExactlyOnce() throws Exception {
        Order order = place(6); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> cancel = () -> { start.await();
                try { service.changeStatus(order.getId(), OrderStatus.CANCELLED); return true; }
                catch (OrderStatusException exception) { return false; }
            };
            var first = executor.submit(cancel); var second = executor.submit(cancel); start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(status(order)).isEqualTo(OrderStatus.CANCELLED); assertThat(stock(product)).isEqualTo(1000);
    }
    @Test void missingProductAndOverflowRollbackEveryRestoration() throws Exception {
        Order order = service.place(Map.of(product.getId(), 2, other.getId(), 3), form());
        var pf = ProductForm.from(products.get(other.getId())); pf.setStockQuantity(Integer.MAX_VALUE); products.update(other.getId(), pf);
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.CANCELLED)).isInstanceOf(OrderStatusException.class).hasMessageContaining("حد مجاز");
        assertThat(stock(product)).isEqualTo(998); assertThat(status(order)).isEqualTo(OrderStatus.WAITING_FOR_PAYMENT);
        productRepository.deleteById(other.getId()); browser.login(); post(order, "CANCELLED");
        assertThat(browser.getHtml(path(order)).body()).contains("حذف شده است", other.getId().toString(), "هیچ موجودی یا وضعیتی تغییر نکرد");
        assertThat(stock(product)).isEqualTo(998); assertThat(status(order)).isEqualTo(OrderStatus.WAITING_FOR_PAYMENT);
    }
    @Test void cancelledConfirmationShowsStatusWithoutRequestingPayment() throws Exception {
        try (var owner = new TestBrowser(port)) {
            owner.postWithCsrf("/products/" + product.getSlug(), "/cart/items", Map.of("productId", product.getId().toString(), "quantity", "2"));
            owner.getHtml("/cart");
            var response = owner.postWithCsrf("/checkout", "/checkout", Map.of("customerName", prefix, "phone", "09123456789",
                    "deliveryArea", "محله", "deliveryAddress", "نشانی نمونه"));
            String confirmation = URI.create(response.headers().firstValue("location").orElseThrow()).getPath();
            String number = confirmation.substring("/orders/".length());
            assertThat(owner.getHtml(confirmation).body()).contains("id=\"payment-instructions\"");
            Order order = orders.findByOrderNumber(number).orElseThrow();
            service.changeStatus(order.getId(), OrderStatus.CANCELLED);
            assertThat(owner.getHtml(confirmation).body()).contains("لغوشده").doesNotContain("id=\"payment-instructions\"", "0000-0000-0000-0000");
            assertThat(stock(product)).isEqualTo(1000);
        }
    }

    @Test void databaseFailureRollsBackStatusAndRestorationWithoutLeakingCustomerData() throws Exception {
        Order order = place(2); browser.login();
        jdbc.execute("alter table noghterangi.orders add constraint admin_cancel_test_failure check (id <> " + order.getId() + " or status <> 'CANCELLED')");
        try {
            post(order, "CANCELLED");
            assertThat(browser.getHtml(path(order)).body()).contains("هیچ تغییری ثبت نشد").doesNotContain("PSQLException", "admin_cancel_test_failure");
            assertThat(status(order)).isEqualTo(OrderStatus.WAITING_FOR_PAYMENT); assertThat(stock(product)).isEqualTo(998);
        } finally { jdbc.execute("alter table noghterangi.orders drop constraint admin_cancel_test_failure"); }
    }
}
