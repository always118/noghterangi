package com.noghterangi.shared;

import com.noghterangi.home.HomeService;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

@Controller
public class HomeController {
    // Only supplied, reviewed group photos; categories without a matching name keep the plain card.
    private static final Map<String, String> CATEGORY_IMAGES = Map.of(
            "دفتر", "category-notebooks-01.jpg", "بطری و قمقمه", "category-bottles-01.jpg",
            "مداد و مدادرنگی", "category-pencils-01.jpg", "نوشت‌افزار", "category-erasers-01.jpg");
    private final HomeService homeService;

    public HomeController(HomeService homeService) { this.homeService = homeService; }

    @GetMapping("/")
    public String home(Model model) {
        var content = homeService.content();
        model.addAttribute("activeCategories", content.categories());
        model.addAttribute("home", content);
        model.addAttribute("categoryImages", CATEGORY_IMAGES);
        return "home";
    }
}
