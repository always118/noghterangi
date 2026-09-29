package com.noghterangi.category;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.noghterangi.admin.AdminTestConfiguration;
import com.noghterangi.admin.TestBrowser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class CategoryIntegrationTest {
    @Autowired CategoryService service;
    @Autowired CategoryRepository repository;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private TestBrowser browser;

    @BeforeEach
    void openBrowser() { browser = new TestBrowser(port); }
    private final String prefix = "test-" + UUID.randomUUID();

    @AfterEach
    void removeOnlyThisTestsCategories() {
        browser.close();
        repository.deleteAll(repository.findAll().stream()
                .filter(category -> category.getSlug().startsWith(prefix)).toList());
    }

    @Test
    void createsNormalizedCategoryWithTimestamps() {
        CategoryForm form = form("  نوشت‌افزار  ", "  " + prefix.toUpperCase(Locale.ROOT) + "  ", true);
        Category saved = service.create(form);
        Category loaded = service.get(saved.getId());
        assertThat(loaded.getName()).isEqualTo("نوشت‌افزار");
        assertThat(loaded.getSlug()).isEqualTo(prefix);
        assertThat(loaded.getDescription()).isEqualTo("توضیحات نمونه");
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isEqualTo(loaded.getCreatedAt());
    }

    @Test
    void rejectsNormalizedDuplicateSlugsEvenForInactiveCategories() {
        service.create(form("اول", prefix, false));
        assertThatThrownBy(() -> service.create(form("دوم", " " + prefix.toUpperCase(Locale.ROOT) + " ", true)))
                .isInstanceOf(DuplicateCategorySlugException.class);
        assertThat(repository.findAll().stream().filter(c -> c.getSlug().equals(prefix))).hasSize(1);
    }

    @Test
    void updatesOwnSlugButRejectsAnotherCategoriesSlugWithoutChangingData() {
        Category first = service.create(form("اول", prefix + "-first", true));
        Category second = service.create(form("دوم", prefix + "-second", true));
        var createdAt = service.get(first.getId()).getCreatedAt();
        service.update(first.getId(), form("ویرایش شده", first.getSlug(), false));
        Category edited = service.get(first.getId());
        assertThat(edited.getName()).isEqualTo("ویرایش شده");
        assertThat(edited.isActive()).isFalse();
        assertThat(edited.getCreatedAt()).isEqualTo(createdAt);
        assertThat(edited.getUpdatedAt()).isAfter(createdAt);
        assertThatThrownBy(() -> service.update(first.getId(), form("نباید ذخیره شود", second.getSlug(), true)))
                .isInstanceOf(DuplicateCategorySlugException.class);
        assertThat(service.get(first.getId()).getName()).isEqualTo("ویرایش شده");
        assertThat(service.get(first.getId()).getSlug()).isEqualTo(first.getSlug());
    }

    @Test
    void databaseEnforcesSlugUniquenessAndFormat() {
        service.create(form("اول", prefix, true));
        assertThatThrownBy(() -> jdbc.update("insert into noghterangi.category (name, slug) values (?, ?)", "تکراری", prefix))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into noghterangi.category (name, slug) values (?, ?)", "نامعتبر", prefix + "--bad"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtersPublicCardsAndNavigationAndEscapesContent() throws Exception {
        Category active = service.create(form("<script>alert(1)</script>", prefix + "-active", true));
        Category inactive = service.create(form("دسته پنهان", prefix + "-hidden", false));
        assertThat(service.findActive()).extracting(Category::getId).contains(active.getId()).doesNotContain(inactive.getId());
        HttpResponse<String> page = get("/");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("id=\"category-" + active.getSlug() + "\"", "href=\"/#category-" + active.getSlug() + "\"", "&lt;script&gt;")
                .doesNotContain(inactive.getSlug(), "<script>alert(1)</script>");
        service.setActive(active.getId(), false);
        assertThat(get("/").body()).doesNotContain(active.getSlug(), inactive.getSlug());
        service.setActive(inactive.getId(), true);
        assertThat(get("/").body()).contains("id=\"category-" + inactive.getSlug() + "\"");
    }

    @Test
    void adminCreatesEditsAndChangesActivationOverHttp() throws Exception {
        browser.login();
        assertThat(get("/admin/categories").statusCode()).isEqualTo(200);
        assertThat(get("/admin/categories/new").body()).contains("name=\"slug\"", "dir=\"rtl\"");
        HttpResponse<String> created = post("/admin/categories", Map.of("name", "دسته جدید", "slug", prefix, "active", "true"));
        assertThat(created.statusCode()).isEqualTo(302);
        assertThat(created.headers().firstValue("location")).hasValueSatisfying(location ->
                assertThat(URI.create(location).getPath().split(";")[0]).isEqualTo("/admin/categories"));
        assertThat(get("/admin/categories").body()).contains("دسته‌بندی ایجاد شد.");
        Category category = repository.findAll().stream().filter(c -> c.getSlug().equals(prefix)).findFirst().orElseThrow();
        String path = "/admin/categories/" + category.getId();
        assertThat(get(path + "/edit").statusCode()).isEqualTo(200);
        assertThat(post(path, Map.of("name", "نام جدید", "slug", prefix + "-edited", "_active", "on")).statusCode()).isEqualTo(302);
        assertThat(service.get(category.getId()).getName()).isEqualTo("نام جدید");
        assertThat(service.get(category.getId()).isActive()).isFalse();
        assertThat(post(path + "/active", Map.of("active", "true")).statusCode()).isEqualTo(302);
        assertThat(service.get(category.getId()).isActive()).isTrue();
        assertThat(post(path + "/active", Map.of("active", "false")).statusCode()).isEqualTo(302);
        assertThat(service.get(category.getId()).isActive()).isFalse();
        assertThat(get("/admin/categories").body()).contains(prefix + "-edited", "نام جدید");
    }

    @Test
    void adminShowsClearValidationErrorsAndPreservesInputs() throws Exception {
        browser.login();
        HttpResponse<String> invalid = post("/admin/categories", Map.of("name", "   ", "slug", prefix + "--bad"));
        assertThat(invalid.statusCode()).isEqualTo(200);
        assertThat(invalid.body()).contains("نام دسته‌بندی را وارد کنید.", "نامک فقط شامل", "is-invalid", prefix + "--bad");
        assertThat(repository.existsBySlug(prefix + "--bad")).isFalse();
        Category existing = service.create(form("اول", prefix, true));
        HttpResponse<String> duplicate = post("/admin/categories", Map.of("name", "نام حفظ شود", "slug", prefix.toUpperCase(Locale.ROOT)));
        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(duplicate.body()).contains("این نامک قبلاً استفاده شده است.", "نام حفظ شود");
        Category other = service.create(form("دوم", prefix + "-other", true));
        HttpResponse<String> duplicateEdit = post("/admin/categories/" + other.getId(), Map.of("name", "ویرایش", "slug", existing.getSlug()));
        assertThat(duplicateEdit.statusCode()).isEqualTo(200);
        assertThat(duplicateEdit.body()).contains("این نامک قبلاً استفاده شده است.", "action=\"/admin/categories/" + other.getId() + "\"");
        assertThat(service.get(other.getId()).getSlug()).isEqualTo(prefix + "-other");
    }

    @Test
    void rejectsInvalidAndOversizedValuesAtTheServiceBoundary() {
        assertThatThrownBy(() -> service.create(form("  ", prefix, true))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.create(form("نام", prefix + "/bad", true))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.create(form("ن".repeat(121), prefix, true))).isInstanceOf(ConstraintViolationException.class);
        CategoryForm tooLong = form("نام", prefix, true);
        tooLong.setDescription("ن".repeat(2001));
        assertThatThrownBy(() -> service.create(tooLong)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void missingCategoryReturns404AndActivationRequiresPost() throws Exception {
        browser.login();
        assertThat(get("/admin/categories/9223372036854775807/edit").statusCode()).isEqualTo(404);
        assertThat(post("/admin/categories/9223372036854775807", Map.of("name", "نام", "slug", prefix)).statusCode()).isEqualTo(404);
        assertThat(post("/admin/categories/9223372036854775807/active", Map.of("active", "false")).statusCode()).isEqualTo(404);
        Category category = service.create(form("نام", prefix, true));
        assertThat(get("/admin/categories/" + category.getId() + "/active").statusCode()).isEqualTo(405);
        assertThat(service.get(category.getId()).isActive()).isTrue();
    }

    @Test
    void supportsOptionalParentAndRejectsSelfParentCyclesAndThirdLevel() {
        Category root = service.create(form("والد", prefix + "-root", true));
        assertThat(root.getParent()).isNull();
        CategoryForm childForm = form("فرزند", prefix + "-child", true);
        childForm.setParentId(root.getId());
        Category child = service.create(childForm);
        assertThat(service.get(child.getId()).getParent().getId()).isEqualTo(root.getId());
        childForm.setParentId(child.getId());
        assertThatThrownBy(() -> service.update(child.getId(), childForm)).isInstanceOf(InvalidCategoryParentException.class);
        CategoryForm invalid = form("نامعتبر", prefix + "-invalid", true);
        invalid.setParentId(child.getId());
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(InvalidCategoryParentException.class);
        CategoryForm rootForm = form("والد", root.getSlug(), true);
        rootForm.setParentId(child.getId());
        assertThatThrownBy(() -> service.update(root.getId(), rootForm)).isInstanceOf(InvalidCategoryParentException.class);
        Category otherRoot = service.create(form("والد دیگر", prefix + "-other-root", true));
        rootForm.setParentId(otherRoot.getId());
        assertThatThrownBy(() -> service.update(root.getId(), rootForm)).isInstanceOf(InvalidCategoryParentException.class);
        childForm.setParentId(null);
        service.update(child.getId(), childForm);
        assertThat(service.get(child.getId()).getParent()).isNull();
        invalid.setParentId(Long.MAX_VALUE);
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(InvalidCategoryParentException.class);
    }

    @Test
    void adminBindsOptionalParentAndShowsClearInvalidParentError() throws Exception {
        browser.login();
        Category root = service.create(form("دسته اصلی", prefix + "-root", true));
        assertThat(post("/admin/categories", Map.of("name", "زیرگروه", "slug", prefix + "-child",
                "parentId", root.getId().toString(), "active", "true")).statusCode()).isEqualTo(302);
        Category child = repository.findAll().stream().filter(c -> c.getSlug().equals(prefix + "-child")).findFirst().orElseThrow();
        assertThat(service.get(child.getId()).getParent().getId()).isEqualTo(root.getId());
        assertThat(get("/admin/categories/" + child.getId() + "/edit").body()).contains("دسته اصلی", "selected=\"selected\"");
        var rejected = post("/admin/categories/" + child.getId(), Map.of("name", "زیرگروه", "slug", child.getSlug(),
                "parentId", child.getId().toString()));
        assertThat(rejected.statusCode()).isEqualTo(200);
        assertThat(rejected.body()).contains("دسته‌بندی نمی‌تواند والد خودش باشد.");
        assertThat(post("/admin/categories/" + child.getId(), Map.of("name", "زیرگروه", "slug", child.getSlug(), "parentId", "")).statusCode()).isEqualTo(302);
    }

    private CategoryForm form(String name, String slug, boolean active) {
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setSlug(slug);
        form.setDescription("توضیحات نمونه");
        form.setActive(active);
        return form;
    }

    private HttpResponse<String> get(String path) throws Exception { return browser.get(path); }
    private HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
        return browser.postWithCsrf("/admin", path, fields);
    }
}
