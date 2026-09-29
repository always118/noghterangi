package com.noghterangi.cart;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CartAdvice {
    @ModelAttribute("cartCount")
    public long count(HttpServletRequest request) {
        var session = request.getSession(false);
        var cart = session == null ? null : (Cart) session.getAttribute("cart");
        return cart == null ? 0 : cart.getCount();
    }
}
