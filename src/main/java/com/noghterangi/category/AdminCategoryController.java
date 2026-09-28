package com.noghterangi.category;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService service;

    public AdminCategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", service.findAll());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "admin/categories/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("categoryForm", CategoryForm.from(service.get(id)));
        model.addAttribute("categoryId", id);
        return "admin/categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CategoryForm categoryForm, BindingResult errors,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) { return "admin/categories/form"; }
        try {
            service.create(categoryForm);
        } catch (DuplicateCategorySlugException exception) {
            errors.rejectValue("slug", "duplicate", exception.getMessage());
            return "admin/categories/form";
        }
        redirect.addFlashAttribute("successMessage", "دسته‌بندی ایجاد شد.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute CategoryForm categoryForm,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        service.get(id);
        model.addAttribute("categoryId", id);
        if (errors.hasErrors()) { return "admin/categories/form"; }
        try {
            service.update(id, categoryForm);
        } catch (DuplicateCategorySlugException exception) {
            errors.rejectValue("slug", "duplicate", exception.getMessage());
            return "admin/categories/form";
        }
        redirect.addFlashAttribute("successMessage", "دسته‌بندی ویرایش شد.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
        service.setActive(id, active);
        redirect.addFlashAttribute("successMessage", "وضعیت دسته‌بندی تغییر کرد.");
        return "redirect:/admin/categories";
    }
}
