package com.noghterangi.media;

import com.noghterangi.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/products/{productId}/images")
public class AdminProductImageController {
    private final MediaService media;
    private final ProductService products;
    public AdminProductImageController(MediaService media, ProductService products) {
        this.media = media; this.products = products;
    }

    @GetMapping
    public String list(@PathVariable Long productId, Model model) {
        model.addAttribute("product", products.get(productId));
        model.addAttribute("images", media.images(productId));
        return "admin/products/images";
    }

    @PostMapping
    public String upload(@PathVariable Long productId,
                         @RequestParam(required = false) List<MultipartFile> files, RedirectAttributes redirect) {
        try {
            media.upload(productId, files);
            redirect.addFlashAttribute("successMessage", "تصاویر بارگذاری شدند.");
        } catch (MediaStorageException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return back(productId);
    }

    @PostMapping("/{imageId}/order")
    public String reorder(@PathVariable Long productId, @PathVariable Long imageId,
                          @RequestParam String sortOrder, RedirectAttributes redirect) {
        try {
            media.reorder(productId, imageId, Integer.parseInt(sortOrder));
            redirect.addFlashAttribute("successMessage", "ترتیب تصویر ذخیره شد.");
        } catch (NumberFormatException exception) {
            redirect.addFlashAttribute("errorMessage", "ترتیب تصویر باید عدد صحیح نامنفی باشد.");
        } catch (MediaStorageException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return back(productId);
    }

    @PostMapping("/{imageId}/delete")
    public String delete(@PathVariable Long productId, @PathVariable Long imageId, RedirectAttributes redirect) {
        media.delete(productId, imageId);
        redirect.addFlashAttribute("successMessage", "تصویر حذف شد.");
        return back(productId);
    }
    private String back(Long id) { return "redirect:/admin/products/" + id + "/images"; }
}
