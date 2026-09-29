package com.noghterangi.category;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAscIdAsc();
    List<Category> findByActiveTrueOrderByNameAscIdAsc();
    List<Category> findByParentIsNullOrderByNameAscIdAsc();
    boolean existsByParentId(Long parentId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
