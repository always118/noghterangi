# Channel import data

این پوشه برای داده‌های بازبینی‌شده‌ی import کانال و کاتالوگ دسته‌هاست. هیچ داده‌ای
به‌صورت خودکار وارد برنامه یا Production نمی‌شود.

## Canonical categories

`category-catalog.json` مرجع قراردادی ۸ دسته‌ی اصلی و ۴۹ زیردسته است. importer دسته‌ای
از JSON نمی‌سازد یا تغییر نمی‌دهد؛ `categorySlug` هر محصول باید در جدول `category` دیتابیس
هدف وجود داشته باشد و همان رکورد دیتابیس مرجع نام، والد و active بودن است.

## Channel product import

`channel-products.json` manifest بازبینی‌شده‌ی استخراج از `noghterangi-export/posts.json` است.
این فایل ۹۰ محصول کاندید و مسیر تصاویر نسبی دارد. رکوردهای دارای `priceReviewRequired` باید
قبل از فروش بررسی شوند و رکوردهای بدون قیمت وارد نمی‌شوند.

سرویس با endpoint محافظت‌شده‌ی admin اجرا می‌شود:

```sh
curl -b admin.cookies -H 'X-CSRF-TOKEN: TOKEN' \
  -F 'manifest=@dev-data/channel-products.json;type=application/json' \
  http://localhost:8080/admin/import/products
```

تصاویر از `PRODUCT_IMPORT_MEDIA_ROOT` (پیش‌فرض `noghterangi-export`) خوانده می‌شوند؛
بنابراین export و پوشه‌ی `media/` باید روی همان host برنامه وجود داشته باشند.

حالت عادی additive و idempotent است و محصولات قبلی را overwrite نمی‌کند. حذف همه‌ی محصولات
فقط با `deleteExisting=true` و بعد از backup مجاز است. import کانال در Production به‌صورت
پیش‌فرض غیرفعال است.
