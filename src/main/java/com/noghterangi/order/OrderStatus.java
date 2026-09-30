package com.noghterangi.order;

public enum OrderStatus {
    WAITING_FOR_PAYMENT, PAID, PREPARING, READY, DELIVERED, CANCELLED;

    public String getLabel() {
        return switch (this) {
            case WAITING_FOR_PAYMENT -> "در انتظار پرداخت";
            case PAID -> "پرداخت‌شده";
            case PREPARING -> "در حال آماده‌سازی";
            case READY -> "آماده تحویل";
            case DELIVERED -> "تحویل‌شده";
            case CANCELLED -> "لغوشده";
        };
    }
}
