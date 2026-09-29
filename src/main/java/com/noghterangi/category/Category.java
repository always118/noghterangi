package com.noghterangi.category;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "category", schema = "noghterangi")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 120)
    private String slug;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private boolean active;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Category() {}

    void update(CategoryForm form) {
        name = form.getName();
        slug = form.getSlug();
        description = form.getDescription();
        active = form.isActive();
    }

    void setParent(Category parent) { this.parent = parent; }
    public Category getParent() { return parent; }

    void setActive(boolean active) { this.active = active; }

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
