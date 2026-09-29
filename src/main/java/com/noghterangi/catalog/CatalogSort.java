package com.noghterangi.catalog;

import org.springframework.data.domain.Sort;

public enum CatalogSort {
    NEWEST("جدیدترین", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    LOWEST_PRICE("ارزان‌ترین", Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"))),
    HIGHEST_PRICE("گران‌ترین", Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id")));

    private final String label;
    private final Sort databaseSort;
    CatalogSort(String label, Sort databaseSort) { this.label = label; this.databaseSort = databaseSort; }
    public String getLabel() { return label; }
    public Sort databaseSort() { return databaseSort; }
}
