package com.noghterangi.order;

import com.noghterangi.product.ProductRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;
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

    @Transactional(readOnly = true)
    public Page<Order> list(OrderStatus status, int page) {
        var pageable = PageRequest.of(page, 20,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Order get(Long id) { return orders.findById(id).orElseThrow(this::missingOrder); }

    public List<OrderStatus> allowedTransitions(OrderStatus current) {
        return switch (current) {
            case WAITING_FOR_PAYMENT -> List.of(OrderStatus.PAID, OrderStatus.CANCELLED);
            case PAID -> List.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> List.of(OrderStatus.READY, OrderStatus.CANCELLED);
            case READY -> List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
            case DELIVERED, CANCELLED -> List.of();
        };
    }

    @Transactional
    public void changeStatus(Long id, OrderStatus next) {
        // This lock serializes updates across sessions and application instances.
        Order order = orders.findForStatusChange(id).orElseThrow(this::missingOrder);
        if (next == null || !allowedTransitions(order.getStatus()).contains(next)) {
            throw new OrderStatusException(order.getStatus() == OrderStatus.CANCELLED
                    ? "این سفارش قبلاً لغو شده است؛ موجودی دوباره افزایش پیدا نمی‌کند."
                    : "این تغییر وضعیت مجاز نیست؛ وضعیت فعلی سفارش را بررسی کنید.");
        }
        if (next == OrderStatus.CANCELLED) {
            // Same ascending product lock order as checkout; no snapshot fields are changed.
            for (OrderItem item : order.getItems().stream().sorted(Comparator.comparing(OrderItem::getProductId)).toList()) {
                var product = products.findForCheckout(item.getProductId()).orElseThrow(() -> new OrderStatusException(
                        "لغو انجام نشد؛ محصول با شناسه " + item.getProductId() + " حذف شده است. ابتدا موجودی را بررسی کنید؛ هیچ موجودی یا وضعیتی تغییر نکرد."));
                try { product.restoreStock(item.getQuantity()); }
                catch (ArithmeticException exception) {
                    throw new OrderStatusException("لغو انجام نشد؛ بازگردانی موجودی محصول با شناسه " + item.getProductId()
                            + " از حد مجاز بیشتر می‌شود. هیچ موجودی یا وضعیتی تغییر نکرد.");
                }
            }
        }
        order.changeStatus(next);
        orders.flush();
    }

    private ResponseStatusException missingOrder() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "سفارش پیدا نشد.");
    }

    private String newNumber() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return "NR-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + random.substring(0, 4) + "-" + random.substring(4, 8) + "-" + random.substring(8, 12) + "-" + random.substring(12);
    }
}
