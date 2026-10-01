# Phase 14 UX verification — 2026-10-01

## Result

Java 21 `mvn clean verify`: **96 tests, zero failures/errors/skipped**. Final packaged app and
all browser checks passed. No migration, dependency, security-policy or business-rule changes.
Phase 14 is DONE; Phase 15 remains NOT_STARTED. No automatic commit.

## Changes and findings

- Cart was hidden inside the closed mobile menu; it now stays visible at every public step.
- Simplified consistent navigation with home, products, categories and search. Escape returns focus;
  quick Escape during opening queues closing after Bootstrap's transition. Native links work without JS.
- Search query/count feedback and existing reset remain clear; child choices include their parent.
  The parent is fetched in the same category query rather than triggering per-category lazy loads.
- Shared cards use correct heading levels, readable prices, two-line names and touch-size title links.
  Available/unavailable labels measure 8.29:1 / 4.92:1 contrast against their background.
- Consistent selects, inputs, labels, focus, pagination and footer touch targets. Removed unused demo CSS.
- A shared hidden rule fixes placeholders remaining visible where display:flex overrode the hidden attribute.
- Product brand text is isolated; unavailable items link back to their category. Cart items link to available details.
- Checkout exposes invalid fields and their errors; the summary receives focus after server validation.
  Mobile total precedes the form; item detail is a native expandable section. Submit explains the next step.
- Confirmation isolates and emphasizes order/card numbers and labels the total as the order amount,
  avoiding a misleading payable label for an order that has already been paid or cancelled.
- Existing lazy media and bounded/batched queries retained; no new large asset or frontend dependency.
  Original supplied photos and logo were not altered. This was a targeted review, not a formal conformance audit.

## Browser verification

Real supplied home/catalog/detail/photos/placeholders, empty cart, empty search and 404 tested at
360, 768 and 1440px with RTL, no horizontal overflow, visible cart, loaded photos and no script errors.
Keyboard menu, fast Escape, focus return, category-anchor focus, skip link, search/query/reset/empty
query, category links and no-JavaScript navigation passed.

Full Home → Search → List → Detail → Add → Cart → Checkout → Confirmation passed with temporary
verification inventory. Real imported stock remains zero; it was never increased for the test.
Cart quantity/price/stock warnings, no-JavaScript cart, server validation, summary focus, keyboard
item disclosure, mobile form order, confirmation privacy and payment presentation passed.

Admin login/logout, categories, products, media management, order filtering/pagination, fulfillment
status flow and cancellation/restocking/retry passed. PostgreSQL assertions verified snapshots and stock.
The initial pagination harness failure was an early DOM count; awaiting the card count resolved it.

All temporary records/images/credentials removed and verification server stopped. Database remains
15 imported products, 17 media, four categories, zero orders. Before/after row fingerprints match;
all 17 stored image files match the original reviewed image bytes. Sample prices, zero stock,
three missing variant-specific photos and absence of real board games remain Phase 13 data limitations.

## Screenshots

Home, catalog, detail, placeholder, empty-cart, empty-search, 404, cart, checkout and confirmation
have captures at each width. Cart/checkout/confirmation captures contain disposable test records.

| View | 360px | 768px | 1440px |
|---|---|---|---|
| home | [360](home-360.png) | [768](home-768.png) | [1440](home-1440.png) |
| catalog | [360](catalog-360.png) | [768](catalog-768.png) | [1440](catalog-1440.png) |
| detail | [360](detail-360.png) | [768](detail-768.png) | [1440](detail-1440.png) |
| placeholder | [360](placeholder-360.png) | [768](placeholder-768.png) | [1440](placeholder-1440.png) |
| empty-cart | [360](empty-cart-360.png) | [768](empty-cart-768.png) | [1440](empty-cart-1440.png) |
| empty-search | [360](empty-search-360.png) | [768](empty-search-768.png) | [1440](empty-search-1440.png) |
| 404 | [360](404-360.png) | [768](404-768.png) | [1440](404-1440.png) |
| cart | [360](cart-360.png) | [768](cart-768.png) | [1440](cart-1440.png) |
| checkout | [360](checkout-360.png) | [768](checkout-768.png) | [1440](checkout-1440.png) |
| confirmation | [360](confirmation-360.png) | [768](confirmation-768.png) | [1440](confirmation-1440.png) |

## Changed files

- `PHASE_STATUS.md`
- `PROJECT_STATUS.md`
- `README.md`
- `src/main/java/com/noghterangi/category/CategoryRepository.java`
- `src/main/resources/static/css/main.css`
- `src/main/resources/templates/cart/index.html`
- `src/main/resources/templates/catalog/list.html`
- `src/main/resources/templates/fragments/catalog-card.html`
- `src/main/resources/templates/fragments/layout.html`
- `src/main/resources/templates/fragments/navigation.html`
- `src/main/resources/templates/order/checkout.html`
- `src/main/resources/templates/order/confirmation.html`
- `src/main/resources/templates/product/detail.html`
- `src/test/java/com/noghterangi/catalog/CatalogIntegrationTest.java`
- `src/test/java/com/noghterangi/home/HomeIntegrationTest.java`
- `src/test/java/com/noghterangi/order/CheckoutIntegrationTest.java`
- `src/main/resources/static/js/public-ux.js` (new)
- `dev-data/verification/phase14/` (new report and screenshots)
