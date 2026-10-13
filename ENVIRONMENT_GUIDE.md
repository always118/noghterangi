# راهنمای محیط‌ها و همگام‌سازی

این پروژه سه محیط جدا دارد. هیچ تسکی نباید فقط با نام «دیتابیس» یا «اپ» اجرا شود؛ قبل از کار باید محیط هدف در گزارش و دستور اجرا مشخص باشد.

| محیط | Spring profile | دیتابیس | رسانه | داده مجاز |
|---|---|---|---|---|
| توسعه | `dev` | `noghterangi` روی پورت 5432، volume پایدار | `MEDIA_STORAGE_PATH` توسعه | داده‌ی نمونه و داده‌ی دستی توسعه |
| تست | `test` | `noghterangi_test` روی پورت 5433، `tmpfs` | موقت | fixtureهای تست؛ هرگز داده‌ی واقعی |
| عملیاتی | `prod` یا `production` | فقط PostgreSQL خارجیِ عملیاتی | storage عملیاتی | داده‌ی واقعی فروشگاه |

## قوانین غیرقابل‌مذاکره

1. `.env` واقعی commit نمی‌شود و رمز توسعه و تست باید متفاوت باشد.
2. `TEST_DB_*` هرگز نباید به دیتابیس توسعه یا عملیاتی اشاره کند. تست‌ها فقط با `TEST_DB_URL` اجرا می‌شوند.
3. `dev-data/`، `noghterangi-export/` و import کانال داده‌ی عملیاتی نیستند. import کانال در Production به‌صورت پیش‌فرض خاموش است.
4. `CHANNEL_IMPORT_ENABLED=true` فقط برای یک import آگاهانه در `dev` یا `test` فعال می‌شود. محیط عملیاتی controller این endpoint را اصلاً ثبت نمی‌کند.
5. حذف محصولات با `deleteExisting=true` فقط پس از backup و تأیید صریح مجاز است.
6. همگام‌سازی schema با Flyway انجام می‌شود؛ همگام‌سازی داده با migration دائمی انجام نمی‌شود. کاتالوگ دسته‌ها از `dev-data/category-catalog.json` خوانده و قبل از هر محیط بررسی می‌شود.
7. برای انتقال داده از توسعه به عملیاتی، ابتدا export بازبینی‌شده، backup عملیاتی و پنجره‌ی تغییر لازم است. هیچ ابزار محلی بدون credentials و تأیید مالک Production حق اتصال یا نوشتن ندارد.

## دسته‌بندی canonical

کاتالوگ canonical شامل ۸ دسته‌ی اصلی و ۴۹ زیردسته است و در
[`dev-data/category-catalog.json`](dev-data/category-catalog.json) نگهداری می‌شود. فایل‌های محصول باید از slugهای همین کاتالوگ استفاده کنند؛ slugهای موقت `dev-*` دیگر مجاز نیستند.

`dev-data/channel-products.json` manifest استخراج کانال است؛ وجود آن به معنی import خودکار یا sync با Production نیست.

## روال هر تسک

1. `PROJECT_STATUS.md`، `PHASE_STATUS.md` و این فایل را بخوان.
2. محیط هدف را صریح بنویس: `dev`، `test` یا `production`.
3. قبل از تغییر، profile، URL دیتابیس، مسیر media و وضعیت backup را بررسی کن.
4. تغییرات schema را با Flyway و تغییرات داده را با importer/endpoint کنترل‌شده انجام بده.
5. بعد از تغییر، همان محیط را verify کن و نتیجه را در status ثبت کن.
6. اگر دسترسی Production وجود ندارد، فقط کد/manifest/راهنما را آماده کن و ادعا نکن که Production sync شده است.

## دستورهای پایه

```sh
# توسعه
docker compose up -d --build

# تست ایزوله
docker compose --profile test up -d --wait postgres-test
# TEST_DB_PASSWORD in .env must match the postgres-test container password.
```

اجرای full test فقط وقتی معتبر است که `TEST_DB_*` دقیقاً با `postgres-test` هماهنگ باشد. برای بررسی ساختار و کاتالوگ، بدون اتصال به Production از `jq` روی manifest canonical استفاده کن.
