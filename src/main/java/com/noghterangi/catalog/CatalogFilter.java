package com.noghterangi.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parses untrusted URL values without binding exceptions or silently widening invalid price ranges. */
public class CatalogFilter {
    public static final int PAGE_SIZE = 12;
    private final String q, category, minPrice, maxPrice;
    private Long minimum, maximum;
    private CatalogSort sort = CatalogSort.NEWEST;
    private boolean available, invalid;
    private int page;
    private final List<String> messages = new ArrayList<>();

    public CatalogFilter(Map<String, String> parameters) {
        q = value(parameters, "q"); category = value(parameters, "category");
        minPrice = value(parameters, "minPrice"); maxPrice = value(parameters, "maxPrice");
        if (q.length() > 160 || category.length() > 120) { reject("عبارت جست‌وجو یا دسته‌بندی بیش از حد طولانی است."); }
        minimum = price(minPrice); maximum = price(maxPrice);
        if (minimum != null && maximum != null && minimum > maximum) { reject("حداقل قیمت نباید بیشتر از حداکثر قیمت باشد."); }
        String requestedSort = value(parameters, "sort");
        if (!requestedSort.isEmpty()) {
            try { sort = CatalogSort.valueOf(requestedSort); }
            catch (IllegalArgumentException exception) { messages.add("ترتیب نامعتبر بود؛ جدیدترین محصولات نمایش داده می‌شوند."); }
        }
        String requestedPage = value(parameters, "page");
        if (!requestedPage.isEmpty()) {
            try {
                page = Integer.parseInt(requestedPage);
                if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE) { throw new NumberFormatException(); }
            } catch (NumberFormatException exception) { page = 0; messages.add("شماره صفحه نامعتبر بود؛ صفحه اول انتخاب شد."); }
        }
        String requestedAvailable = value(parameters, "available");
        available = "true".equals(requestedAvailable);
        if (!requestedAvailable.isEmpty() && !requestedAvailable.equals("true") && !requestedAvailable.equals("false")) {
            messages.add("گزینه موجودی نامعتبر بود؛ همه وضعیت‌های موجودی نمایش داده می‌شوند.");
        }
    }
    private static String value(Map<String, String> values, String key) { return values.getOrDefault(key, "").trim(); }
    private Long price(String value) {
        if (value.isEmpty()) { return null; }
        try {
            long result = Long.parseLong(value);
            if (result < 0) { throw new NumberFormatException(); }
            return result;
        } catch (NumberFormatException exception) { reject("قیمت باید عدد صحیح نامنفی به تومان و در محدوده مجاز باشد."); return null; }
    }
    void reject(String message) { invalid = true; if (!messages.contains(message)) { messages.add(message); } }
    public String getQ() { return q; }
    public String getCategory() { return category; }
    public String getMinPrice() { return minPrice; }
    public String getMaxPrice() { return maxPrice; }
    public Long getMinimum() { return minimum; }
    public Long getMaximum() { return maximum; }
    public CatalogSort getSort() { return sort; }
    public boolean isAvailable() { return available; }
    public boolean isInvalid() { return invalid; }
    public int getPage() { return page; }
    public List<String> getMessages() { return List.copyOf(messages); }
}
