package com.noghterangi.shared;

import com.noghterangi.home.HomeService;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) { this.homeService = homeService; }

    @GetMapping("/")
    public String home(Model model) {
        var content = homeService.content();
        model.addAttribute("activeCategories", content.categories());
        model.addAttribute("home", content);
        return "home";
    }
}
