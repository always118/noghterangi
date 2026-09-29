package com.noghterangi.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByDisplayOrderAscIdAsc();
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
}
