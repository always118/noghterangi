package com.noghterangi.cart;

import com.noghterangi.product.Product;
import com.noghterangi.product.ProductRepository;
import com.noghterangi.media.MediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartService {
    private final ProductRepository products;
    private final MediaService media;
    public CartService(ProductRepository products, MediaService media) {
        this.products = products; this.media = media;
    }

    public void change(Cart cart, long id, int quantity, String action) {
        synchronized (cart) {
            int current = cart.items().getOrDefault(id, 0);
            long requested = switch (action) {
                case "add" -> (long) current + quantity;
                case "increase" -> (long) current + 1;
                case "decrease" -> (long) current - 1;
                case "update" -> quantity;
                default -> throw new IllegalArgumentException("عملیات سبد خرید معتبر نیست.");
            };
            if (!action.equals("add") && current == 0) {
                throw new IllegalArgumentException("این محصول در سبد خرید نیست.");
            }
            if ((action.equals("add") || action.equals("update")) && quantity < 1 || requested < 1) {
                throw new IllegalArgumentException("تعداد باید حداقل ۱ باشد؛ برای حذف از دکمه حذف استفاده کنید.");
            }
            Product product = products.findById(id).orElseThrow(() ->
                    new IllegalArgumentException("محصول پیدا نشد؛ می‌توانید آن را از سبد حذف کنید."));
            String warning = warning(product, requested);
            if (warning != null) { throw new IllegalArgumentException(warning); }
            cart.put(id, (int) requested);
        }
    }

    public View resolve(Cart cart) {
        var quantities = cart.items();
        var images = media.primaryImages(new ArrayList<>(quantities.keySet()));
        List<Line> lines = new ArrayList<>();
        long subtotal = 0;
        boolean valid = true;
        boolean overflow = false;
        for (var entry : quantities.entrySet()) {
            Product product = products.findById(entry.getKey()).orElse(null);
            String warning = product == null ? "این محصول دیگر در دسترس نیست." : warning(product, entry.getValue());
            Long total = null;
            if (product != null) {
                try { total = Math.multiplyExact(product.getPrice(), (long) entry.getValue()); }
                catch (ArithmeticException exception) { warning = "مبلغ این ردیف بیش از حد مجاز است؛ تعداد را کاهش دهید."; overflow = true; }
            }
            if (total != null && !overflow) {
                try { subtotal = Math.addExact(subtotal, total); }
                catch (ArithmeticException exception) { overflow = true; }
            }
            valid &= warning == null;
            lines.add(new Line(entry.getKey(), entry.getValue(), product, images.get(entry.getKey()), total, warning));
        }
        return new View(lines, overflow ? null : subtotal, valid && !overflow);
    }

    private String warning(Product product, long quantity) {
        if (!product.isActive() || !product.getCategory().isActive()) { return "این محصول در حال حاضر قابل خرید نیست."; }
        if (product.getStockQuantity() <= 0) { return "این محصول ناموجود است."; }
        if (quantity > product.getStockQuantity()) { return "تعداد درخواستی بیشتر از موجودی است؛ موجودی فعلی: " + product.getStockQuantity(); }
        return null;
    }
    public record Line(long productId, int quantity, Product product, Long imageId, Long total, String warning) {}
    public record View(List<Line> lines, Long subtotal, boolean valid) {}
}
