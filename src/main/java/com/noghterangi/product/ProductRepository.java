package com.noghterangi.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByDisplayOrderAscIdAsc();
    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryIdOrderByDisplayOrderAscIdAsc(Long categoryId);
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);
    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlugAndActiveTrueAndCategoryActiveTrue(String slug);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    Optional<Product> findForCheckout(@org.springframework.data.repository.query.Param("id") Long id);
    @EntityGraph(attributePaths = "category")
    List<Product> findTop8ByActiveTrueAndCategoryActiveTrueOrderByCreatedAtDescIdDesc();
    @EntityGraph(attributePaths = "category")
    List<Product> findTop8ByActiveTrueAndCategoryActiveTrueAndFeaturedTrueOrderByCreatedAtDescIdDesc();
    @EntityGraph(attributePaths = "category")
    List<Product> findTop8ByActiveTrueAndCategoryActiveTrueAndTypeOrderByCreatedAtDescIdDesc(ProductType type);
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
}
