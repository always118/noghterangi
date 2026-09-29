package com.noghterangi.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
    @Query("""
            select m from Media m where m.product.id in :productIds and not exists (
                select earlier.id from Media earlier where earlier.product.id = m.product.id
                and (earlier.sortOrder < m.sortOrder or (earlier.sortOrder = m.sortOrder and earlier.id < m.id)))
            """)
    List<Media> findPrimaryImages(List<Long> productIds);
    Optional<Media> findByIdAndProductId(Long id, Long productId);
}
