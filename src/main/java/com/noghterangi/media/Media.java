package com.noghterangi.media;

import com.noghterangi.product.Product;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "media", schema = "noghterangi")
public class Media {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String storedFileName;
    @Column(nullable = false, length = 255)
    private String originalFileName;
    @Column(nullable = false, length = 32)
    private String contentType;
    @Column(nullable = false)
    private Long fileSize;
    @Column(nullable = false)
    private Integer sortOrder;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected Media() {}
    Media(Product product, MediaStorageService.StoredImage stored, int order) {
        this.product = product;
        storedFileName = stored.storedFileName();
        originalFileName = stored.originalFileName();
        contentType = stored.contentType();
        fileSize = stored.fileSize();
        sortOrder = order;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Long getId() { return id; }
    public String getStoredFileName() { return storedFileName; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public Integer getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Product getProduct() { return product; }
}
