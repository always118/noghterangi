package com.noghterangi.catalog;

import com.noghterangi.category.CategoryService;
import com.noghterangi.media.MediaService;
import com.noghterangi.product.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.ArrayList;
import java.util.Map;

@Controller
public class CatalogController {
    private final CatalogService catalog;
    private final CategoryService categories;
    private final MediaService media;
    public CatalogController(CatalogService catalog, CategoryService categories, MediaService media) {
        this.catalog = catalog; this.categories = categories; this.media = media;
    }

    @GetMapping("/products")
    public String list(@RequestParam Map<String, String> parameters, Model model) {
        var filter = new CatalogFilter(parameters);
        var page = catalog.search(filter);
        model.addAttribute("filter", filter);
        model.addAttribute("products", page);
        model.addAttribute("activeCategories", categories.findActive());
        model.addAttribute("sorts", CatalogSort.values());
        model.addAttribute("primaryImages", media.primaryImages(page.getContent().stream().map(Product::getId).toList()));
        var links = new ArrayList<PageLink>();
        int start = Math.max(0, Math.min(page.getNumber() - 1, page.getTotalPages() - 3));
        for (int number = start; number < Math.min(page.getTotalPages(), start + 3); number++) {
            links.add(new PageLink(number + 1, pageUrl(filter, number), number == page.getNumber()));
        }
        model.addAttribute("pageLinks", links);
        model.addAttribute("previousUrl", page.hasPrevious() ? pageUrl(filter, Math.min(page.getNumber() - 1, Math.max(0, page.getTotalPages() - 1))) : null);
        model.addAttribute("nextUrl", page.hasNext() ? pageUrl(filter, page.getNumber() + 1) : null);
        model.addAttribute("firstUrl", pageUrl(filter, 0));
        return "catalog/list";
    }

    private String pageUrl(CatalogFilter filter, int page) {
        return UriComponentsBuilder.fromPath("/products")
                .queryParam("q", "{q}").queryParam("category", "{category}")
                .queryParam("minPrice", "{minPrice}").queryParam("maxPrice", "{maxPrice}")
                .queryParam("available", filter.isAvailable()).queryParam("sort", filter.getSort().name())
                .queryParam("page", page).encode().buildAndExpand(Map.of(
                        "q", filter.getQ(), "category", filter.getCategory(),
                        "minPrice", filter.getMinPrice(), "maxPrice", filter.getMaxPrice())).toUriString();
    }
    public record PageLink(int number, String url, boolean current) {}
}
