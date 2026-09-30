package com.noghterangi.order;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orders;
    public AdminOrderController(OrderService orders) { this.orders = orders; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") String page,
                       Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusFilter", status);
        try {
            OrderStatus filter = status.isBlank() ? null : OrderStatus.valueOf(status);
            int number = Integer.parseInt(page);
            if (number < 0) { throw new IllegalArgumentException(); }
            model.addAttribute("orders", orders.list(filter, number));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("orders", Page.empty());
            model.addAttribute("errorMessage", "وضعیت یا شماره صفحه معتبر نیست.");
        }
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        Order order = orders.get(id);
        model.addAttribute("order", order);
        model.addAttribute("transitions", orders.allowedTransitions(order.getStatus()));
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/status")
    public String update(@PathVariable Long id, @RequestParam(defaultValue = "") String status, RedirectAttributes flash) {
        OrderStatus next;
        try { next = OrderStatus.valueOf(status); }
        catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("errorMessage", "وضعیت انتخاب‌شده معتبر نیست.");
            return "redirect:/admin/orders/" + id;
        }
        try {
            orders.changeStatus(id, next);
            flash.addFlashAttribute("successMessage", next == OrderStatus.CANCELLED
                    ? "سفارش لغو شد و موجودی کالاها بازگردانده شد." : "وضعیت سفارش به‌روز شد.");
        } catch (OrderStatusException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (DataAccessException | TransactionException exception) {
            // Never log customer fields or database exception details containing their values.
            log.warn("Admin status transaction failed for order {}", id);
            flash.addFlashAttribute("errorMessage", "تغییر وضعیت یا بازگردانی موجودی انجام نشد؛ هیچ تغییری ثبت نشد. دوباره تلاش کنید.");
        }
        return "redirect:/admin/orders/" + id;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String missing(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "سفارش پیدا نشد.");
        return "redirect:/admin/orders";
    }
}
