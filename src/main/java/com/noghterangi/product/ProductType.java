package com.noghterangi.product;

public enum ProductType {
    STATIONERY("لوازم‌تحریر"), EDUCATIONAL("آموزشی"), BOARD_GAME("بازی رومیزی"), BOOK("کتاب"), OTHER("سایر");

    private final String label;
    ProductType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
