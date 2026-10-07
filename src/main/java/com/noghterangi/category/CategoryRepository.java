package com.noghterangi.category;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlugAndActiveTrue(String slug);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "parent")
    List<Category> findAllByOrderByNameAscIdAsc();
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "parent")
    List<Category> findByActiveTrueOrderByNameAscIdAsc();
    List<Category> findByParentIsNullOrderByNameAscIdAsc();
    List<Category> findByParentIdOrderByNameAscIdAsc(Long parentId);
    List<Category> findByActiveTrueAndParentIsNullOrderByNameAscIdAsc();
    Optional<Category> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByParentId(Long parentId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
