package com.noghterangi.order;

import com.noghterangi.product.ProductRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Locale;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;
    private final Validator validator;
    private final PaymentInstructions payment;
    public OrderService(ProductRepository products, OrderRepository orders, Validator validator, PaymentInstructions payment) {
        this.products = products; this.orders = orders; this.validator = validator; this.payment = payment;
    }

    @Transactional
    public Order place(Map<Long, Integer> quantities, CheckoutForm form) {
        if (quantities == null || quantities.isEmpty()) { throw new CheckoutException("سبد خرید خالی است."); }
        if (form == null || !validator.validate(form).isEmpty()) { throw new CheckoutException("اطلاعات سفارش کامل نیست؛ فرم را بررسی کنید."); }
        if (!payment.isConfigured()) { throw new CheckoutException("ثبت سفارش موقتاً در دسترس نیست؛ اطلاعات پرداخت فروشگاه تنظیم نشده است."); }
        Order order = new Order(newNumber(), form);
        // Consistent lock order avoids deadlocks between multi-product checkouts.
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            Integer quantity = entry.getValue();
            if (quantity == null || quantity < 1) { throw new CheckoutException("تعداد کالا معتبر نیست؛ سبد خرید را اصلاح کنید."); }
            var product = products.findForCheckout(entry.getKey()).orElseThrow(() ->
                    new CheckoutException("این محصول دیگر در دسترس نیست؛ سبد خرید را اصلاح کنید."));
            if (!product.isActive() || !product.getCategory().isActive()) {
                throw new CheckoutException("این محصول دیگر در دسترس نیست؛ سبد خرید را اصلاح کنید.");
            }
            if (product.getStockQuantity() < quantity) {
                throw new CheckoutException("موجودی یکی از محصولات کافی نیست؛ تعداد را در سبد خرید اصلاح کنید.");
            }
            try { order.addItem(product.getId(), product.getName(), product.getPrice(), quantity); }
            catch (ArithmeticException exception) { throw new CheckoutException("مبلغ سفارش بیش از حد مجاز است؛ تعداد کالاها را کاهش دهید."); }
            product.decreaseStock(quantity);
        }
        return orders.saveAndFlush(order);
    }

    private String newNumber() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return "NR-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + random.substring(0, 4) + "-" + random.substring(4, 8) + "-" + random.substring(8, 12) + "-" + random.substring(12);
    }
}
