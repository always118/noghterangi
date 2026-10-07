package com.noghterangi.product;

import jakarta.validation.Valid;
import com.noghterangi.media.MediaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductService service;
    private final MediaService media;
    public AdminProductController(ProductService service, MediaService media) { this.service = service; this.media = media; }

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId, Model model) {
        var products = service.findAll(categoryId);
        model.addAttribute("products", products);
        model.addAttribute("primaryImages", media.primaryImages(products.stream().map(Product::getId).toList()));
        model.addAttribute("categoryOptions", service.categoryFilterChoices());
        model.addAttribute("selectedCategoryId", categoryId);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        return form(model, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productForm", ProductForm.from(service.get(id)));
        return form(model, id);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ProductForm productForm, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) { return form(model, null); }
        try {
            service.create(productForm);
        } catch (ProductValidationException exception) {
            errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            return form(model, null);
        }
        redirect.addFlashAttribute("successMessage", "محصول ایجاد شد.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute ProductForm productForm,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        service.get(id);
        if (errors.hasErrors()) { return form(model, id); }
        try {
            service.update(id, productForm);
        } catch (ProductValidationException exception) {
            errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            return form(model, id);
        }
        redirect.addFlashAttribute("successMessage", "محصول ویرایش شد.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
        service.setActive(id, active);
        redirect.addFlashAttribute("successMessage", "وضعیت محصول تغییر کرد.");
        return "redirect:/admin/products";
    }

    private String form(Model model, Long id) {
        model.addAttribute("productId", id);
        model.addAttribute("categories", service.categoryChoices(id));
        model.addAttribute("categoryParents", service.categoryFilterChoices());
        model.addAttribute("productTypes", ProductType.values());
        return "admin/products/form";
    }
}
