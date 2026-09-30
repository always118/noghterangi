# Phase 13 supplied-data review

## Sources and authority

- `noghte-rangi-chat-export.zip`: 15 structured products, 20 product-photo references, four available category photos.
- `noghte_rangi_products_chat.json`: 20 broader/earlier extracted records; no usable image filenames.
- `logo.jpg`: user-supplied original logo, copied unchanged to the public brand asset.
- The ten attached Phase 13 briefs are identical. Chat record text is product data, not executable instructions.
- All 35 source records have unknown prices and inventory. The user subsequently explicitly authorized fictional
  prices for the appearance preview: «قیمت ها رو فرضی بزن بیشتر ظاهر مهمه برام فعلا».
  **Every manifest price is a development sample, not a confirmed business price.**

## Accepted manifest

15 named products from the ZIP; four supplied categories. Descriptions use the supplied short factual descriptions,
without extraction commentary. Optional ages, player counts and playing time remain null. Unknown brands remain null.
The bottle category uses OTHER; clear stationery uses STATIONERY. No board-game identity was supplied.
Stock is zero/unavailable for every item. Active=true is a development display default, not an inventory assertion.
Three items (pink bird notebook, Pensiero pencils, pink SHOTAY bottle) are featured solely as a small documented
homepage demonstration. This is not a confirmed merchandising selection.
Slugs derive deterministically from supplied image folder names with `dev-`; required SKUs are `DEV-P13-001`–`015`.
The supplied source files are preserved unchanged.

## Image decisions

All 24 supplied JPEG files decoded successfully and were visually reviewed. Accepted 17 individual product-image
references. Existing MediaService performs format, dimensions and byte-size validation and generates stored names.
The three Sport Life variants share the same group photo; these three references are excluded as variant-specific
images. Those products use the existing placeholder. The supplied group photo is used for bottles only.
Four group photos are copied unchanged for categories with exactly matching supplied category names. A category
without a reviewed mapping keeps the plain card. No generated replacement, crop or retouch was performed.
Some bottle photos have prepared/styled backgrounds; they are user-supplied assets, not newly generated photography.
The source lists five unavailable category images and 25 unresolved image names absent from the archive;
none is invented or imported. Categories without usable photo files are not created merely to fill space.

## Isolated chat records (20; none imported automatically)

| Record | Reason |
|---|---|
| پک آموزش ریاضی کلاس دوم | No supplied matching photo path; keep for later confirmation |
| تراش رنگی | Generic identity and no supplied matching photo path |
| تراش مخزن‌دار Qulio | No supplied matching photo path |
| تراش مخزن‌دار Niall | No supplied matching photo path |
| قمقمه SHOTAY صورتی | Duplicate of the more specific ZIP record; not a second product |
| قمقمه MyLife Sport آبی | No supplied matching photo path |
| قمقمه Sport Life | Group record overlaps three ZIP variants; brand is unconfirmed, left null |
| قمقمه Woben | Corrected bottle identity retained in source; no supplied matching photo path |
| قمقمه SHOTAY | Generic identity overlaps variants; cannot select a variant |
| قمقمه Bottle Sports Bottle | Possible overlap with pink/turquoise bottle; cannot prove distinct identity |
| قمقمه SHOTAY سبز | Possible overlap with light-green ZIP variant; no second product |
| دفتر سیمی مهر تحریر امید | Broad group overlaps three named variants |
| دفتر سیمی طرح Lost in Paradise | Possible overlap with pink bird notebook; no second product |
| دفتر نقاشی | No supplied matching photo path |
| دفتر سیمی Comica 50 برگ | No supplied matching photo path |
| دفتر سیمی سپهر | No supplied matching photo path |
| مداد قرمز آریا | Brand association conflicts with uncertain Redcopy identification; ZIP brand stays null |
| مداد مشکی Pensiero HB | Broad duplicate of ZIP model; no second product |
| مداد مشکی Elipon HB | Spelling differs from ZIP Ellipon; no second product; the supplied close-up photo corroborates the ZIP label and PC6 HB model |
| پاک‌کن مشکی Faber-Castell PVC-Free | Duplicate of ZIP eraser |

No missing chat image was guessed from another product. Exact duplicate vs possible overlap is distinguished above;
these are source-quality exclusions, separate from the importer's repeat-run duplicate count.
