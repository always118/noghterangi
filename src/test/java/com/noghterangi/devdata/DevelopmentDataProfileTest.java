package com.noghterangi.devdata;

import com.noghterangi.category.*;
import com.noghterangi.product.*;
import com.noghterangi.media.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DevelopmentDataProfileTest {
    @Test void requiresExplicitOptInAndDevOrTestAndAlwaysExcludesProduction() {
        check(false, false, "dev"); check(false, true); check(false, true, "production");
        check(false, true, "prod"); check(false, true, "dev", "production"); check(false, true, "test", "prod");
        check(true, true, "dev"); check(true, true, "test");
    }
    private void check(boolean present, boolean enabled, String... profiles) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profiles);
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("import-test", Map.of("app.dev-data.enabled", enabled)));
            context.registerBean(CategoryService.class, () -> mock(CategoryService.class));
            context.registerBean(CategoryRepository.class, () -> mock(CategoryRepository.class));
            context.registerBean(ProductService.class, () -> mock(ProductService.class));
            context.registerBean(ProductRepository.class, () -> mock(ProductRepository.class));
            context.registerBean(MediaService.class, () -> mock(MediaService.class));
            context.registerBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));
            context.register(DevelopmentDataImporter.class); context.refresh();
            assertThat(context.getBeansOfType(DevelopmentDataImporter.class)).hasSize(present ? 1 : 0);
        }
    }
}
