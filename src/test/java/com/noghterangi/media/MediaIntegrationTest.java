package com.noghterangi.media;

import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import com.noghterangi.category.*;
import com.noghterangi.product.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class MediaIntegrationTest {
    @Autowired MediaService service;
    @Autowired MediaRepository repository;
    @Autowired MediaStorageService storage;
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryService categories;
    @Autowired CategoryRepository categoryRepository;
    @Autowired PlatformTransactionManager transactions;
    @Value("${app.media.storage-path}") String storagePath;
    @LocalServerPort int port;
    Long productId, otherId, categoryId;
    TestBrowser browser;

    @BeforeEach void setUp() {
        browser = new TestBrowser(port);
        String prefix = "media-test-" + UUID.randomUUID();
        CategoryForm category = new CategoryForm(); category.setName("دسته آزمون تصویر"); category.setSlug(prefix);
        categoryId = categories.create(category).getId();
        ProductForm product = new ProductForm();
        product.setName("محصول آزمون تصویر"); product.setSlug(prefix); product.setSku(prefix);
        product.setPrice(100L); product.setStockQuantity(2); product.setCategoryId(categoryId); product.setType(ProductType.BOOK);
        productId = products.create(product).getId();
        product.setSlug(prefix + "-2"); product.setSku(prefix + "-2"); otherId = products.create(product).getId();
    }
    @AfterEach void cleanUp() {
        browser.close();
        for (Long id : List.of(productId, otherId)) {
            service.images(id).forEach(image -> service.delete(id, image.getId()));
            productRepository.deleteById(id);
        }
        categoryRepository.deleteById(categoryId);
    }

    @Test void multipleImagesBelongToProductAndOrderingSelectsPrimary() throws Exception {
        service.upload(productId, List.of(ImageFixtures.image("jpeg"), ImageFixtures.image("png")));
        var images = service.images(productId);
        assertThat(images).hasSize(2);
        assertThat(images).extracting(Media::getSortOrder).containsExactly(0, 1);
        assertThat(images.getFirst().getProduct().getId()).isEqualTo(productId);
        assertThat(images.getFirst().getCreatedAt()).isNotNull();
        assertThat(service.images(otherId)).isEmpty();
        assertThat(service.primaryImages(List.of(productId, otherId))).containsEntry(productId, images.getFirst().getId()).doesNotContainKey(otherId);
        service.reorder(productId, images.getFirst().getId(), 5);
        assertThat(service.primaryImages(List.of(productId))).containsEntry(productId, images.getLast().getId());
        assertThatThrownBy(() -> service.reorder(productId, images.getFirst().getId(), -1)).isInstanceOf(MediaStorageException.class);
        assertThatThrownBy(() -> service.delete(otherId, images.getFirst().getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.reorder(otherId, images.getFirst().getId(), 0)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void servesExactPublicImageBytesWithCorrectTypesAndMissing404() throws Exception {
        var jpeg = ImageFixtures.image("jpeg"); var png = ImageFixtures.image("png");
        service.upload(productId, List.of(jpeg, png));
        try (HttpClient client = HttpClient.newHttpClient()) {
            var images = service.images(productId);
            for (int i = 0; i < images.size(); i++) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/media/" + images.get(i).getId())).build(), HttpResponse.BodyHandlers.ofByteArray());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo(i == 0 ? jpeg.getBytes() : png.getBytes());
                assertThat(response.headers().firstValue("content-type")).contains(i == 0 ? "image/jpeg" : "image/png");
                assertThat(response.headers().firstValue("x-content-type-options")).contains("nosniff");
            }
        }
        assertThat(browser.get("/media/9223372036854775807").statusCode()).isEqualTo(404);
        var first = service.images(productId).getFirst(); storage.delete(first.getStoredFileName());
        assertThat(browser.get("/media/" + first.getId()).statusCode()).isEqualTo(404);
        service.delete(productId, first.getId());
        assertThat(repository.findById(first.getId())).isEmpty();
    }

    @Test void removesRecordAndPhysicalFileWithoutAffectingOtherImages() throws Exception {
        service.upload(productId, List.of(ImageFixtures.image("png"), ImageFixtures.image("jpeg")));
        var images = service.images(productId); var first = images.getFirst();
        service.delete(productId, first.getId());
        assertThat(repository.findById(first.getId())).isEmpty();
        assertThat(Path.of(storagePath, first.getStoredFileName())).doesNotExist();
        assertThat(service.content(images.getLast().getId()).bytes()).isNotEmpty();
    }

    @Test void failedBatchAndDatabaseRollbackCleanNewFilesButRolledBackDeletionKeepsFile() throws Exception {
        var png = ImageFixtures.image("png");
        Set<String> before = storedNames();
        assertThatThrownBy(() -> service.upload(productId, List.of(png, new MockMultipartFile("files", "bad.svg", "image/svg+xml", "bad".getBytes()))))
                .isInstanceOf(MediaStorageException.class);
        assertThat(service.images(productId)).isEmpty(); assertThat(storedNames()).isEqualTo(before);
        var transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> { service.upload(productId, List.of(png)); status.setRollbackOnly(); });
        assertThat(service.images(productId)).isEmpty(); assertThat(storedNames()).isEqualTo(before);
        service.upload(productId, List.of(png)); var image = service.images(productId).getFirst();
        transaction.executeWithoutResult(status -> { service.delete(productId, image.getId()); status.setRollbackOnly(); });
        assertThat(service.content(image.getId()).bytes()).isEqualTo(png.getBytes());
    }

    @Test void realHttpUploadRequiresAuthenticationAndCsrfAndAdminCanOrderAndDelete() throws Exception {
        String path = "/admin/products/" + productId + "/images";
        var png = ImageFixtures.image("png");
        assertThat(browser.get(path).statusCode()).isEqualTo(302);
        String anonymousToken = TestBrowser.csrf(browser.get("/admin/login").body());
        assertThat(browser.upload(path, anonymousToken, List.of(png)).statusCode()).isEqualTo(302);
        assertThat(service.images(productId)).isEmpty();
        browser.login();
        assertThat(browser.get(path).body()).contains("dir=\"rtl\"", "multipart/form-data", "هنوز تصویری ندارد");
        assertThat(browser.upload(path, null, List.of(png)).statusCode()).isEqualTo(403);
        assertThat(browser.upload(path, TestBrowser.csrf(browser.get(path).body()), List.of(png, ImageFixtures.image("jpeg"))).statusCode()).isEqualTo(302);
        var image = service.images(productId).getFirst();
        assertThat(service.images(productId)).hasSize(2);
        assertThat(browser.get(path).body()).contains("تصویر اصلی", "/media/" + image.getId());
        assertThat(browser.get("/admin/products").body()).contains("/media/" + image.getId(), "بدون تصویر");
        assertThat(browser.post(path + "/" + image.getId() + "/delete", Map.of()).statusCode()).isEqualTo(403);
        assertThat(browser.post(path + "/" + image.getId() + "/order", Map.of("sortOrder", "9")).statusCode()).isEqualTo(403);
        assertThat(browser.postWithCsrf(path, path + "/" + image.getId() + "/order", Map.of("sortOrder", "9")).statusCode()).isEqualTo(302);
        assertThat(service.images(productId).getLast().getId()).isEqualTo(image.getId());
        assertThat(browser.postWithCsrf(path, path + "/" + image.getId() + "/order", Map.of("sortOrder", "1.2")).statusCode()).isEqualTo(302);
        assertThat(browser.get(path).body()).contains("ترتیب تصویر باید عدد صحیح");
        try (var anonymous = new TestBrowser(port)) {
            assertThat(anonymous.postWithCsrf("/admin/login", path + "/" + image.getId() + "/delete", Map.of()).statusCode()).isEqualTo(302);
        }
        assertThat(browser.postWithCsrf(path, path + "/" + image.getId() + "/delete", Map.of()).statusCode()).isEqualTo(302);
        assertThat(repository.findById(image.getId())).isEmpty();
    }

    @Test void realHttpInvalidAndOversizedUploadsShowPersianErrorsWithoutSaving() throws Exception {
        browser.login(); String path = "/admin/products/" + productId + "/images";
        var invalid = new MockMultipartFile("files", "bad.svg", "image/svg+xml", "bad".getBytes());
        assertThat(browser.upload(path, TestBrowser.csrf(browser.get(path).body()), List.of(invalid)).statusCode()).isEqualTo(302);
        assertThat(browser.get(path).body()).contains("فقط تصویر JPEG یا PNG");
        var oversized = new MockMultipartFile("files", "big.png", "image/png", new byte[LocalMediaStorageService.MAX_BYTES + 1]);
        var response = browser.upload(path, TestBrowser.csrf(browser.get(path).body()), List.of(oversized));
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("حجم تصویر بیش از حد مجاز").doesNotContain(storagePath, "Exception");
        assertThat(service.images(productId)).isEmpty();
    }

    private Set<String> storedNames() throws Exception {
        try (var files = Files.list(Path.of(storagePath))) {
            return new HashSet<>(files.map(p -> p.getFileName().toString()).toList());
        }
    }
}
