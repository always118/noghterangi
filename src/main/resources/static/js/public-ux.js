// Progressive enhancement only: links and forms still work without JavaScript.
const publicMenu = document.getElementById('main-navigation');
const menuToggle = document.querySelector('[aria-controls="main-navigation"]');
if (publicMenu && menuToggle) {
    const closeMenu = () => {
        if (menuToggle.getAttribute('aria-expanded') !== 'true') return;
        const collapse = bootstrap.Collapse.getOrCreateInstance(publicMenu, {toggle: false});
        // Bootstrap ignores hide() during opening; remember a quick Escape or anchor click.
        if (publicMenu.classList.contains('collapsing')) {
            publicMenu.addEventListener('shown.bs.collapse', () => collapse.hide(), {once: true});
        } else {
            collapse.hide();
        }
    };
    publicMenu.closest('nav').addEventListener('keydown', event => {
        if (event.key === 'Escape' && getComputedStyle(menuToggle).display !== 'none') {
            closeMenu();
            menuToggle.focus();
        }
    });
    publicMenu.addEventListener('click', event => {
        const link = event.target.closest('a');
        if (!link || getComputedStyle(menuToggle).display === 'none') return;
        const target = new URL(link.href);
        if (target.pathname === location.pathname && target.hash) {
            const destination = document.getElementById(target.hash.slice(1));
            publicMenu.addEventListener('hidden.bs.collapse', () => {
                if (destination) {
                    if (!destination.hasAttribute('tabindex')) destination.tabIndex = -1;
                    destination.focus();
                }
            }, {once: true});
            closeMenu();
        }
    });
}
const errorSummary = document.querySelector('[data-validation-summary]');
if (errorSummary) errorSummary.focus();
