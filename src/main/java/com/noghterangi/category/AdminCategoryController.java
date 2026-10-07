package com.noghterangi.category;

import com.noghterangi.media.MediaService;
import com.noghterangi.media.MediaStorageException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService service;
    private final MediaService media;

    public AdminCategoryController(CategoryService service, MediaService media) {
        this.service = service; this.media = media;
    }

    @ModelAttribute("parentCategories")
    public java.util.List<Category> parentCategories() { return service.findRoots(); }

    @GetMapping
    public String list(@RequestParam(required = false) Long parentId, Model model) {
        List<Category> roots = service.findRoots();
        Long requestedParentId = parentId;
        Category selectedParent = roots.stream()
                .filter(category -> category.getId().equals(requestedParentId))
                .findFirst()
                .orElse(null);
        if (parentId != null && selectedParent == null) {
            parentId = null;
        }
        model.addAttribute("categories", selectedParent == null ? roots : service.findChildren(parentId));
        model.addAttribute("selectedParentId", parentId);
        model.addAttribute("selectedParent", selectedParent);
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
        model.addAttribute("categoryImage", media.categoryImage(id).orElse(null));
        return "admin/categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CategoryForm categoryForm, BindingResult errors,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) { return "admin/categories/form"; }
        try {
            service.create(categoryForm);
        } catch (InvalidCategoryParentException exception) {
            errors.rejectValue("parentId", "invalid", exception.getMessage());
            return "admin/categories/form";
        } catch (DuplicateCategorySlugException exception) {
            errors.rejectValue("slug", "duplicate", exception.getMessage());
            return "admin/categories/form";
        }
        redirect.addFlashAttribute("successMessage", "دسته‌بندی ایجاد شد.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute CategoryForm categoryForm,
                         BindingResult errors, Model model,
                         @RequestParam(required = false) MultipartFile image,
                         RedirectAttributes redirect) {
        service.get(id);
        model.addAttribute("categoryId", id);
        model.addAttribute("categoryImage", media.categoryImage(id).orElse(null));
        if (errors.hasErrors()) { return "admin/categories/form"; }
        try {
            service.update(id, categoryForm);
        } catch (InvalidCategoryParentException exception) {
            errors.rejectValue("parentId", "invalid", exception.getMessage());
            return "admin/categories/form";
        } catch (DuplicateCategorySlugException exception) {
            errors.rejectValue("slug", "duplicate", exception.getMessage());
            return "admin/categories/form";
        }
        try {
            if (image != null && !image.isEmpty()) { media.replaceCategoryImage(id, image); }
        } catch (MediaStorageException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        }
        redirect.addFlashAttribute("successMessage", "دسته‌بندی ویرایش شد.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/image/delete")
    public String deleteImage(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            media.deleteCategoryImage(id);
            redirect.addFlashAttribute("successMessage", "تصویر دسته‌بندی حذف شد.");
        } catch (MediaStorageException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/categories/" + id + "/edit";
    }

    @PostMapping("/{id}/image")
    public String uploadImage(@PathVariable Long id, @RequestParam MultipartFile image,
                              RedirectAttributes redirect) {
        try {
            media.replaceCategoryImage(id, image);
            redirect.addFlashAttribute("successMessage", "تصویر دسته‌بندی بارگذاری شد.");
        } catch (MediaStorageException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/categories/" + id + "/edit";
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
        service.setActive(id, active);
        redirect.addFlashAttribute("successMessage", "وضعیت دسته‌بندی تغییر کرد.");
        return "redirect:/admin/categories";
    }
}
