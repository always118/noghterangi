package com.noghterangi.catalog;

import com.noghterangi.channel.ChannelProductImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/** Server-rendered admin page for the protected development/test catalog operations. */
@Controller
@RequestMapping("/admin/catalog")
@Profile("!prod & !production")
@ConditionalOnProperty(name = "app.catalog-management.enabled", havingValue = "true")
public class AdminCatalogManagementPageController {
    private final CatalogManagementService catalog;
    private final ChannelProductImportService channelImport;
    private final boolean channelImportEnabled;

    public AdminCatalogManagementPageController(CatalogManagementService catalog,
            ChannelProductImportService channelImport,
            @Value("${app.channel-import.enabled:false}") boolean channelImportEnabled) {
        this.catalog = catalog;
        this.channelImport = channelImport;
        this.channelImportEnabled = channelImportEnabled;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("channelImportEnabled", channelImportEnabled);
        return "admin/catalog/index";
    }

    @PostMapping("/products/clear-ui")
    public String clearProducts(@RequestParam String confirmation, RedirectAttributes redirect) {
        try {
            var report = catalog.deleteAllProducts(confirmation);
            redirect.addFlashAttribute("successMessage", "محصولات پاک شدند: "
                    + report.deletedProducts() + " محصول و " + report.deletedMedia() + " تصویر.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", "تأیید حذف صحیح نیست.");
        }
        return "redirect:/admin/catalog";
    }

    @PostMapping("/categories/clear-ui")
    public String clearCategories(@RequestParam String confirmation, RedirectAttributes redirect) {
        try {
            var report = catalog.deleteAllCategories(confirmation);
            redirect.addFlashAttribute("successMessage", "دسته‌ها پاک شدند: "
                    + report.deletedCategories() + " دسته و " + report.deletedMedia() + " تصویر.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", "تأیید حذف صحیح نیست.");
        } catch (IllegalStateException exception) {
            redirect.addFlashAttribute("errorMessage", "ابتدا همه محصولات را حذف کنید.");
        }
        return "redirect:/admin/catalog";
    }

    @PostMapping("/categories/import-ui")
    public String importCategories(@RequestParam("catalog") MultipartFile file, RedirectAttributes redirect) {
        try {
            var report = catalog.importCategories(file.getBytes());
            redirect.addFlashAttribute("successMessage", "دسته‌ها وارد شدند: "
                    + report.created() + " جدید و " + report.existing() + " موجود.");
        } catch (IOException | RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", "ورود دسته‌ها انجام نشد: " + safeMessage(exception));
        }
        return "redirect:/admin/catalog";
    }

    @PostMapping("/products/import-ui")
    public String importProducts(@RequestParam("manifest") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean deleteExisting,
            @RequestParam(defaultValue = "false") boolean allowPriceReviewRequired,
            RedirectAttributes redirect) {
        try {
            var report = channelImport.importManifest(file.getBytes(), deleteExisting, allowPriceReviewRequired);
            redirect.addFlashAttribute("successMessage", "محصولات کانال پردازش شدند: "
                    + report.imported() + " وارد، " + report.skipped() + " رد یا نادیده گرفته، "
                    + report.duplicates() + " تکراری.");
        } catch (IOException | RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", "ورود محصولات انجام نشد: " + safeMessage(exception));
        }
        return "redirect:/admin/catalog";
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "خطای نامشخص" : message;
    }
}
