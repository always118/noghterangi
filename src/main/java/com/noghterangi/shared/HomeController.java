package com.noghterangi.shared;

import com.noghterangi.category.CategoryService;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CategoryService categoryService;

    public HomeController(CategoryService categoryService) { this.categoryService = categoryService; }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activeCategories", categoryService.findActive());
        return "home";
    }
}
