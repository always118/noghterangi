package com.noghterangi.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", schema = "noghterangi")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber;
    @Column(nullable = false, length = 160)
    private String customerName;
    @Column(nullable = false, length = 11)
    private String phone;
    @Column(nullable = false, length = 160)
    private String deliveryArea;
    @Column(nullable = false, length = 1000)
    private String deliveryAddress;
    @Column(length = 1000)
    private String customerNote;
    @Column(nullable = false)
    private Long totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private OrderStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}
    Order(String number, CheckoutForm form) {
        orderNumber = number; customerName = form.getCustomerName(); phone = form.getPhone();
        deliveryArea = form.getDeliveryArea(); deliveryAddress = form.getDeliveryAddress();
        customerNote = form.getCustomerNote(); totalAmount = 0L;
        status = OrderStatus.WAITING_FOR_PAYMENT; createdAt = Instant.now();
    }
    void addItem(Long productId, String name, long price, int quantity) {
        long total = Math.multiplyExact(price, (long) quantity);
        totalAmount = Math.addExact(totalAmount, total);
        items.add(new OrderItem(this, productId, name, price, quantity, total));
    }
    void changeStatus(OrderStatus status) { this.status = status; }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getDeliveryArea() { return deliveryArea; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getCustomerNote() { return customerNote; }
    public Long getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
