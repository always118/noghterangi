package com.noghterangi.order;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items", schema = "noghterangi")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    // Deliberately not an entity association: historical snapshots survive product deletion.
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false, length = 160)
    private String productName;
    @Column(nullable = false)
    private Long unitPrice;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private Long lineTotal;

    protected OrderItem() {}
    OrderItem(Order order, Long productId, String name, long price, int quantity, long total) {
        this.order = order; this.productId = productId; productName = name;
        unitPrice = price; this.quantity = quantity; lineTotal = total;
    }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public Long getLineTotal() { return lineTotal; }
}
