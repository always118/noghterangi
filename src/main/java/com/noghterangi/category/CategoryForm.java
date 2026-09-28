package com.noghterangi.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public class CategoryForm {
    @NotBlank(message = "نام دسته‌بندی را وارد کنید.")
    @Size(max = 120, message = "نام باید حداکثر ۱۲۰ نویسه باشد.")
    private String name;
    @NotBlank(message = "نامک را وارد کنید.")
    @Size(max = 120, message = "نامک باید حداکثر ۱۲۰ نویسه باشد.")
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "نامک فقط شامل حروف انگلیسی، عدد و خط تیره بین کلمات باشد.")
    private String slug;
    @Size(max = 2000, message = "توضیحات باید حداکثر ۲۰۰۰ نویسه باشد.")
    private String description;
    private boolean active = true;

    public static CategoryForm from(Category category) {
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setSlug(category.getSlug());
        form.setDescription(category.getDescription());
        form.setActive(category.isActive());
        return form;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.strip(); }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug == null ? null : slug.strip().toLowerCase(Locale.ROOT); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? null : description.strip(); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
