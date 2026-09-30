package com.noghterangi.order;

import jakarta.validation.constraints.*;

public class CheckoutForm {
    @NotBlank(message = "نام و نام خانوادگی را وارد کنید.")
    @Size(max = 160, message = "نام حداکثر ۱۶۰ نویسه باشد.")
    private String customerName;
    @NotBlank(message = "شماره موبایل را وارد کنید.")
    @Pattern(regexp = "09[0-9]{9}", message = "شماره موبایل را به صورت ۰۹ و ۱۱ رقم وارد کنید.")
    private String phone;
    @NotBlank(message = "محله یا محدوده تحویل را وارد کنید.")
    @Size(max = 160, message = "محله حداکثر ۱۶۰ نویسه باشد.")
    private String deliveryArea;
    @NotBlank(message = "نشانی تحویل را وارد کنید.")
    @Size(max = 1000, message = "نشانی حداکثر ۱۰۰۰ نویسه باشد.")
    private String deliveryAddress;
    @Size(max = 1000, message = "یادداشت حداکثر ۱۰۰۰ نویسه باشد.")
    private String customerNote;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { customerName = strip(value); }
    public String getPhone() { return phone; }
    public void setPhone(String value) {
        if (value == null) { phone = null; return; }
        StringBuilder normalized = new StringBuilder();
        for (char c : value.strip().toCharArray()) {
            if (c >= '۰' && c <= '۹') { c = (char) ('0' + c - '۰'); }
            else if (c >= '٠' && c <= '٩') { c = (char) ('0' + c - '٠'); }
            normalized.append(c);
        }
        phone = normalized.toString();
    }
    public String getDeliveryArea() { return deliveryArea; }
    public void setDeliveryArea(String value) { deliveryArea = strip(value); }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String value) { deliveryAddress = strip(value); }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String value) { customerNote = strip(value); }
    private String strip(String value) { return value == null ? null : value.strip(); }
}
