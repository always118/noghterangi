package com.noghterangi.category;

public class DuplicateCategorySlugException extends RuntimeException {
    public DuplicateCategorySlugException() {
        super("این نامک قبلاً استفاده شده است. نامک دیگری انتخاب کنید.");
    }
}
