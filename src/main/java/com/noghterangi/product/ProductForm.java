package com.noghterangi.product;

import jakarta.validation.constraints.*;
import java.util.Locale;

public class ProductForm {
    @NotBlank(message = "نام محصول را وارد کنید.")
    @Size(max = 160, message = "نام حداکثر ۱۶۰ نویسه باشد.")
    private String name;
    @NotBlank(message = "نامک را وارد کنید.")
    @Size(max = 160, message = "نامک حداکثر ۱۶۰ نویسه باشد.")
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "نامک فقط شامل حروف انگلیسی کوچک، عدد و خط تیره بین کلمات باشد.")
    private String slug;
    @NotBlank(message = "کد کالا را وارد کنید.")
    @Size(max = 64, message = "کد کالا حداکثر ۶۴ نویسه باشد.")
    private String sku;
    @Size(max = 120, message = "برند حداکثر ۱۲۰ نویسه باشد.")
    private String brand;
    @Size(max = 500, message = "توضیح کوتاه حداکثر ۵۰۰ نویسه باشد.")
    private String shortDescription;
    @Size(max = 10000, message = "توضیحات حداکثر ۱۰۰۰۰ نویسه باشد.")
    private String description;
    @NotNull(message = "قیمت را وارد کنید.")
    @PositiveOrZero(message = "قیمت نباید منفی باشد.")
    private Long price;
    @NotNull(message = "موجودی را وارد کنید.")
    @PositiveOrZero(message = "موجودی نباید منفی باشد.")
    private Integer stockQuantity = 0;
    private boolean active = true;
    private boolean featured;
    @NotNull(message = "ترتیب نمایش را وارد کنید.")
    @PositiveOrZero(message = "ترتیب نمایش نباید منفی باشد.")
    private Integer displayOrder = 0;
    @NotNull(message = "نوع محصول را انتخاب کنید.")
    private ProductType type;
    private Integer minimumAge;
    private Integer maximumAge;
    private Integer minimumPlayers;
    private Integer maximumPlayers;
    private Integer playingTimeMinutes;
    @NotNull(message = "دسته‌بندی را انتخاب کنید.")
    @Positive(message = "دسته‌بندی معتبر انتخاب کنید.")
    private Long categoryId;

    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setSlug(product.getSlug());
        form.setSku(product.getSku());
        form.setBrand(product.getBrand());
        form.setShortDescription(product.getShortDescription());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setStockQuantity(product.getStockQuantity());
        form.setActive(product.isActive());
        form.setFeatured(product.isFeatured());
        form.setDisplayOrder(product.getDisplayOrder());
        form.setType(product.getType());
        form.setMinimumAge(product.getMinimumAge());
        form.setMaximumAge(product.getMaximumAge());
        form.setMinimumPlayers(product.getMinimumPlayers());
        form.setMaximumPlayers(product.getMaximumPlayers());
        form.setPlayingTimeMinutes(product.getPlayingTimeMinutes());
        form.setCategoryId(product.getCategory().getId());
        return form;
    }

    @AssertTrue(message = "سن‌ها نباید منفی باشند و حداکثر سن نباید کمتر از حداقل سن باشد.")
    public boolean isAgeRangeValid() {
        return type != ProductType.BOARD_GAME || ((minimumAge == null || minimumAge >= 0)
                && (maximumAge == null || maximumAge >= 0)
                && (minimumAge == null || maximumAge == null || maximumAge >= minimumAge));
    }

    @AssertTrue(message = "تعداد بازیکنان باید مثبت باشد و حداکثر کمتر از حداقل نباشد.")
    public boolean isPlayerRangeValid() {
        return type != ProductType.BOARD_GAME || ((minimumPlayers == null || minimumPlayers > 0)
                && (maximumPlayers == null || maximumPlayers > 0)
                && (minimumPlayers == null || maximumPlayers == null || maximumPlayers >= minimumPlayers));
    }

    @AssertTrue(message = "زمان بازی باید عددی مثبت باشد.")
    public boolean isPlayingTimeValid() {
        return type != ProductType.BOARD_GAME || playingTimeMinutes == null || playingTimeMinutes > 0;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.strip(); }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug == null ? null : slug.strip().toLowerCase(Locale.ROOT); }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku == null ? null : sku.strip().toUpperCase(Locale.ROOT); }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand == null ? null : brand.strip(); }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription == null ? null : shortDescription.strip(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? null : description.strip(); }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public ProductType getType() { return type; }
    public void setType(ProductType type) { this.type = type; }
    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }
    public Integer getMaximumAge() { return maximumAge; }
    public void setMaximumAge(Integer maximumAge) { this.maximumAge = maximumAge; }
    public Integer getMinimumPlayers() { return minimumPlayers; }
    public void setMinimumPlayers(Integer minimumPlayers) { this.minimumPlayers = minimumPlayers; }
    public Integer getMaximumPlayers() { return maximumPlayers; }
    public void setMaximumPlayers(Integer maximumPlayers) { this.maximumPlayers = maximumPlayers; }
    public Integer getPlayingTimeMinutes() { return playingTimeMinutes; }
    public void setPlayingTimeMinutes(Integer playingTimeMinutes) { this.playingTimeMinutes = playingTimeMinutes; }
}
