package com.noghterangi.media;

import com.noghterangi.product.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class MediaService {
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private final MediaRepository repository;
    private final MediaStorageService storage;
    private final ProductService products;

    public MediaService(MediaRepository repository, MediaStorageService storage, ProductService products) {
        this.repository = repository; this.storage = storage; this.products = products;
    }

    public List<Media> images(Long productId) {
        products.get(productId);
        return repository.findByProductIdOrderBySortOrderAscIdAsc(productId);
    }

    public Map<Long, Long> primaryImages(List<Long> productIds) {
        Map<Long, Long> result = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Media media : repository.findPrimaryImages(productIds)) {
                result.putIfAbsent(media.getProduct().getId(), media.getId());
            }
        }
        return result;
    }

    @Transactional
    public void upload(Long productId, List<MultipartFile> files) {
        var product = products.get(productId);
        if (files == null || files.isEmpty() || files.size() > 10) {
            throw new MediaStorageException("در هر بار بارگذاری، ۱ تا ۱۰ تصویر انتخاب کنید.");
        }
        var existing = repository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        long next = existing.stream().mapToLong(Media::getSortOrder).max().orElse(-1) + 1;
        if (next + files.size() > Integer.MAX_VALUE) {
            throw new MediaStorageException("ابتدا ترتیب تصاویر قبلی را کاهش دهید.");
        }
        List<String> created = new ArrayList<>();
        // Database rollback (including commit failure) must not leave newly uploaded files behind.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) { created.forEach(MediaService.this::deleteFileSafely); }
            }
        });
        for (MultipartFile file : files) {
            var stored = storage.store(file);
            created.add(stored.storedFileName());
            repository.save(new Media(product, stored, (int) next++));
        }
    }

    @Transactional
    public void reorder(Long productId, Long imageId, int order) {
        if (order < 0) { throw new MediaStorageException("ترتیب تصویر باید عدد صحیح نامنفی باشد."); }
        owned(productId, imageId).setSortOrder(order);
    }

    @Transactional
    public void delete(Long productId, Long imageId) {
        Media media = owned(productId, imageId);
        String name = media.getStoredFileName();
        repository.delete(media);
        // Keep the file if the database deletion rolls back.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteFileSafely(name); }
        });
    }

    public ImageContent content(Long id) {
        Media media = repository.findById(id).orElseThrow(this::missing);
        return new ImageContent(media.getContentType(), storage.read(media.getStoredFileName()));
    }

    private Media owned(Long productId, Long id) {
        return repository.findByIdAndProductId(id, productId).orElseThrow(this::missing);
    }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "تصویر پیدا نشد."); }
    private void deleteFileSafely(String name) {
        try { storage.delete(name); }
        catch (MediaStorageException exception) {
            log.warn("Media file cleanup failed for generated file {}; manual cleanup required", name);
        }
    }
    public record ImageContent(String contentType, byte[] bytes) {}
}
