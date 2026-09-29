const gallery = document.querySelector('[data-product-gallery]');
if (gallery) {
    const primary = gallery.querySelector('#primary-image');
    const showPlaceholder = image => {
        image.hidden = true;
        image.previousElementSibling.hidden = false;
    };
    gallery.addEventListener('error', event => {
        if (event.target instanceof HTMLImageElement) showPlaceholder(event.target);
    }, true);
    gallery.querySelectorAll('img').forEach(image => {
        if (image.complete && image.naturalWidth === 0) showPlaceholder(image);
    });
    gallery.querySelectorAll('[data-gallery-thumbnail]').forEach(link => {
        link.addEventListener('click', event => {
            event.preventDefault();
            primary.hidden = false;
            primary.previousElementSibling.hidden = true;
            primary.alt = link.querySelector('img').alt;
            primary.src = link.href;
            gallery.querySelectorAll('[data-gallery-thumbnail]').forEach(item => {
                item.setAttribute('aria-current', String(item === link));
            });
            if (primary.complete && primary.naturalWidth === 0) showPlaceholder(primary);
        });
    });
}
