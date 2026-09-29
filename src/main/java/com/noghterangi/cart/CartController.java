package com.noghterangi.cart;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService service;
    public CartController(CartService service) { this.service = service; }

    static Cart cart(HttpSession session) {
        synchronized (session) {
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) { cart = new Cart(); session.setAttribute("cart", cart); }
            return cart;
        }
    }
    @GetMapping
    public String show(HttpSession session, Model model) {
        model.addAttribute("cartView", service.resolve(cart(session)));
        return "cart/index";
    }
    @PostMapping("/items")
    public String add(@RequestParam(defaultValue = "") String productId,
                      @RequestParam(defaultValue = "") String quantity,
                      HttpSession session, RedirectAttributes flash) {
        return change(productId, quantity, "add", session, flash);
    }
    @PostMapping("/items/{id}")
    public String change(@PathVariable String id, @RequestParam(defaultValue = "1") String quantity,
                         @RequestParam(defaultValue = "update") String action,
                         HttpSession session, RedirectAttributes flash) {
        try {
            long productId = Long.parseLong(id);
            Cart cart = cart(session);
            if (action.equals("remove")) { cart.remove(productId); }
            else { service.change(cart, productId, Integer.parseInt(quantity), action); }
            flash.addFlashAttribute("successMessage", "سبد خرید به‌روز شد.");
        } catch (NumberFormatException exception) {
            flash.addFlashAttribute("errorMessage", "شناسه محصول و تعداد باید عدد صحیح معتبر باشند.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }
    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes flash) {
        cart(session).clear();
        flash.addFlashAttribute("successMessage", "سبد خرید خالی شد.");
        return "redirect:/cart";
    }
}
