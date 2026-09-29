package com.noghterangi.media;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
    List<Media> findByProductIdInOrderBySortOrderAscIdAsc(List<Long> productIds);
    Optional<Media> findByIdAndProductId(Long id, Long productId);
}
