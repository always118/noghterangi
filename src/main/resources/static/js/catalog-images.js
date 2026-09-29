// A missing physical file uses the same placeholder as a product without image metadata.
function showCatalogImagePlaceholder(image) {
    if (!image.matches('img[data-catalog-image]')) return;
    image.hidden = true;
    image.previousElementSibling.hidden = false;
}
document.addEventListener('error', event => {
    if (event.target instanceof HTMLImageElement) showCatalogImagePlaceholder(event.target);
}, true);
document.querySelectorAll('img[data-catalog-image]').forEach(image => {
    if (image.complete && image.naturalWidth === 0) showCatalogImagePlaceholder(image);
});
