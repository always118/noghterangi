package com.noghterangi.product;

import com.noghterangi.category.Category;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "product", schema = "noghterangi")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, length = 160)
    private String slug;
    @Column(nullable = false, length = 64)
    private String sku;
    @Column(length = 120)
    private String brand;
    @Column(length = 500)
    private String shortDescription;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private Long price;
    @Column(nullable = false)
    private Integer stockQuantity;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private boolean featured;
    @Column(nullable = false)
    private Integer displayOrder;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductType type;
    private Integer minimumAge;
    private Integer maximumAge;
    private Integer minimumPlayers;
    private Integer maximumPlayers;
    private Integer playingTimeMinutes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Product() {}

    void update(ProductForm form, Category category) {
        this.category = category;
        name = form.getName();
        slug = form.getSlug();
        sku = form.getSku();
        brand = form.getBrand();
        shortDescription = form.getShortDescription();
        description = form.getDescription();
        price = form.getPrice();
        stockQuantity = form.getStockQuantity();
        active = form.isActive();
        featured = form.isFeatured();
        displayOrder = form.getDisplayOrder();
        type = form.getType();
        minimumAge = form.getType() == ProductType.BOARD_GAME ? form.getMinimumAge() : null;
        maximumAge = form.getType() == ProductType.BOARD_GAME ? form.getMaximumAge() : null;
        minimumPlayers = form.getType() == ProductType.BOARD_GAME ? form.getMinimumPlayers() : null;
        maximumPlayers = form.getType() == ProductType.BOARD_GAME ? form.getMaximumPlayers() : null;
        playingTimeMinutes = form.getType() == ProductType.BOARD_GAME ? form.getPlayingTimeMinutes() : null;
    }

    public void decreaseStock(int quantity) {
        if (quantity < 1 || quantity > stockQuantity) { throw new IllegalArgumentException("Invalid stock decrease"); }
        stockQuantity -= quantity;
    }

    void setActive(boolean active) { this.active = active; }
    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getSku() { return sku; }
    public String getBrand() { return brand; }
    public String getShortDescription() { return shortDescription; }
    public String getDescription() { return description; }
    public Long getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public boolean isActive() { return active; }
    public boolean isFeatured() { return featured; }
    public Integer getDisplayOrder() { return displayOrder; }
    public ProductType getType() { return type; }
    public Integer getMinimumAge() { return minimumAge; }
    public Integer getMaximumAge() { return maximumAge; }
    public Integer getMinimumPlayers() { return minimumPlayers; }
    public Integer getMaximumPlayers() { return maximumPlayers; }
    public Integer getPlayingTimeMinutes() { return playingTimeMinutes; }
}
