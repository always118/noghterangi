package com.noghterangi.product;

import com.noghterangi.media.MediaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductDetailController {
    private final ProductService products;
    private final MediaService media;
    public ProductDetailController(ProductService products, MediaService media) {
        this.products = products; this.media = media;
    }

    @GetMapping("/products/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Product product = products.getPublic(slug);
        model.addAttribute("product", product);
        model.addAttribute("images", media.images(product.getId()));
        String summary = product.getShortDescription();
        model.addAttribute("metaDescription", summary == null || summary.isBlank() ? product.getName() : summary);
        boolean gameSpecifications = product.getType() == ProductType.BOARD_GAME &&
                (product.getMinimumAge() != null || product.getMaximumAge() != null || product.getMinimumPlayers() != null
                        || product.getMaximumPlayers() != null || product.getPlayingTimeMinutes() != null);
        model.addAttribute("gameSpecifications", gameSpecifications);
        return "product/detail";
    }
}
