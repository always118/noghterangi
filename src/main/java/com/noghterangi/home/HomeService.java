package com.noghterangi.home;

import com.noghterangi.category.Category;
import com.noghterangi.category.CategoryRepository;
import com.noghterangi.media.MediaService;
import com.noghterangi.product.Product;
import com.noghterangi.product.ProductRepository;
import com.noghterangi.product.ProductType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class HomeService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final MediaService media;

    public HomeService(CategoryRepository categories, ProductRepository products, MediaService media) {
        this.categories = categories; this.products = products; this.media = media;
    }

    public HomeContent content() {
        var newest = products.findTop8ByActiveTrueAndCategoryActiveTrueOrderByCreatedAtDescIdDesc();
        var featured = products.findTop8ByActiveTrueAndCategoryActiveTrueAndFeaturedTrueOrderByCreatedAtDescIdDesc();
        var games = products.findTop8ByActiveTrueAndCategoryActiveTrueAndTypeOrderByCreatedAtDescIdDesc(ProductType.BOARD_GAME);
        var ids = Stream.of(newest, featured, games).flatMap(List::stream).map(Product::getId).distinct().toList();
        var activeCategories = categories.findByActiveTrueAndParentIsNullOrderByNameAscIdAsc();
        var categoryIds = activeCategories.stream().map(Category::getId).toList();
        return new HomeContent(activeCategories, newest, featured, games,
                media.primaryImages(ids), media.primaryCategoryImages(categoryIds));
    }

    public record HomeContent(List<Category> categories, List<Product> newest, List<Product> featured,
                              List<Product> games, Map<Long, Long> images,
                              Map<Long, Long> categoryImages) {}
}
