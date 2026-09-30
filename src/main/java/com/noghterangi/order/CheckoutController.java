package com.noghterangi.order;

import com.noghterangi.cart.Cart;
import com.noghterangi.cart.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashSet;
import java.util.Set;

@Controller
public class CheckoutController {
    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final CartService carts;
    private final OrderService orders;
    private final OrderRepository repository;
    private final PaymentInstructions payment;
    public CheckoutController(CartService carts, OrderService orders, OrderRepository repository, PaymentInstructions payment) {
        this.carts = carts; this.orders = orders; this.repository = repository; this.payment = payment;
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, RedirectAttributes flash, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null || cart.getCount() == 0) { return empty(flash); }
        var view = carts.resolve(cart);
        if (!view.valid()) {
            flash.addFlashAttribute("errorMessage", model.containsAttribute("errorMessage") ? model.getAttribute("errorMessage")
                    : "سبد خرید نیاز به اصلاح دارد؛ موجودی و وضعیت کالاها را بررسی کنید.");
            return "redirect:/cart";
        }
        if (!model.containsAttribute("checkoutForm")) { model.addAttribute("checkoutForm", new CheckoutForm()); }
        model.addAttribute("cartView", view);
        model.addAttribute("paymentConfigured", payment.isConfigured());
        return "order/checkout";
    }

    @PostMapping("/checkout")
    public String place(@Valid @ModelAttribute CheckoutForm checkoutForm, BindingResult binding,
                        HttpSession session, RedirectAttributes flash) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) { return empty(flash); }
        // Covers commit and clearing, so concurrent submissions of the same cart cannot create two orders.
        synchronized (cart) {
            if (cart.getCount() == 0) { return empty(flash); }
            if (binding.hasErrors()) {
                flash.addFlashAttribute("checkoutForm", checkoutForm);
                flash.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "checkoutForm", binding);
                return "redirect:/checkout";
            }
            try {
                Order order = orders.place(cart.items(), checkoutForm);
                // The service proxy has committed before returning. Failures leave the cart intact.
                cart.clear();
                remember(session, order.getOrderNumber());
                return "redirect:/orders/" + order.getOrderNumber();
            } catch (CheckoutException exception) {
                flash.addFlashAttribute("errorMessage", exception.getMessage());
            } catch (DataAccessException | TransactionException exception) {
                log.warn("Checkout transaction failed; cart retained", exception);
                flash.addFlashAttribute("errorMessage", "ثبت سفارش انجام نشد؛ سبد شما محفوظ است. دوباره تلاش کنید.");
            }
            flash.addFlashAttribute("checkoutForm", checkoutForm);
            return "redirect:/checkout";
        }
    }

    @GetMapping("/orders/{number}")
    public String confirmation(@PathVariable String number, HttpSession session, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (!owns(session, number)) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        var order = repository.findByOrderNumber(number).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("order", order); model.addAttribute("payment", payment);
        return "order/confirmation";
    }
    private String empty(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "سبد خرید خالی است؛ ابتدا محصولی انتخاب کنید.");
        return "redirect:/cart";
    }
    private void remember(HttpSession session, String number) {
        synchronized (session) {
            @SuppressWarnings("unchecked")
            Set<String> existing = (Set<String>) session.getAttribute("placedOrders");
            Set<String> numbers = existing == null ? new HashSet<>() : new HashSet<>(existing);
            numbers.add(number); session.setAttribute("placedOrders", numbers);
        }
    }
    private boolean owns(HttpSession session, String number) {
        synchronized (session) {
            Object existing = session.getAttribute("placedOrders");
            return existing instanceof Set<?> numbers && numbers.contains(number);
        }
    }
}
