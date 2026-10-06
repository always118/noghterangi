(() => {
    'use strict';

    const form = document.querySelector('[data-image-editor-form]');
    const input = form?.querySelector('input[type="file"]');
    const editor = document.querySelector('#image-editor');
    if (!form || !input || !editor || typeof Cropper === 'undefined') return;

    const image = document.querySelector('#image-editor-image');
    const preview = document.querySelector('#image-editor-preview');
    const progress = document.querySelector('#image-editor-progress');
    const target = { width: Number(form.dataset.editorWidth), height: Number(form.dataset.editorHeight) };
    const maxFiles = Number(form.dataset.editorMaxFiles || 1);
    const kind = form.dataset.editorKind === 'category' ? 'category' : 'product';
    editor.dataset.editorKind = kind;
    let cropper;
    let objectUrl;
    let files = [];
    let processed = [];
    let currentIndex = 0;

    const show = () => { editor.hidden = false; document.body.classList.add('image-editor-open'); };
    const hide = () => { editor.hidden = true; document.body.classList.remove('image-editor-open'); };
    const destroyCropper = () => {
        if (cropper) { cropper.destroy(); cropper = null; }
        if (objectUrl) { URL.revokeObjectURL(objectUrl); objectUrl = null; }
        image.removeAttribute('src');
    };
    const cancel = () => {
        destroyCropper();
        files = [];
        processed = [];
        currentIndex = 0;
        input.value = '';
        input.removeAttribute('data-crop-ready');
        hide();
    };
    const updatePreview = () => {
        if (!cropper) return;
        const canvas = cropper.getCroppedCanvas({
            maxWidth: target.width,
            maxHeight: target.height,
            fillColor: '#ffffff',
            imageSmoothingEnabled: true,
            imageSmoothingQuality: 'high'
        });
        if (canvas) preview.src = canvas.toDataURL('image/jpeg', 0.85);
    };
    const openCurrent = () => {
        destroyCropper();
        const file = files[currentIndex];
        if (!file) return;
        progress.textContent = files.length > 1 ? `تصویر ${currentIndex + 1} از ${files.length}` : '';
        objectUrl = URL.createObjectURL(file);
        image.onload = () => {
            cropper = new Cropper(image, {
                aspectRatio: target.width / target.height,
                viewMode: 1,
                autoCropArea: 1,
                responsive: true,
                background: false,
                movable: true,
                zoomable: true,
                cropBoxMovable: true,
                cropBoxResizable: true,
                ready: updatePreview,
                cropend: updatePreview
            });
        };
        image.src = objectUrl;
        const title = document.querySelector('#image-editor-title');
        const help = document.querySelector('#image-editor-help');
        title.textContent = kind === 'category' ? 'برش تصویر دسته‌بندی' : 'برش تصویر محصول';
        help.textContent = kind === 'category'
            ? 'برش ۳:۲؛ تصویر را جابه‌جا یا با لمس/چرخ ماوس بزرگ‌نمایی کنید.'
            : 'برش ۴:۵؛ تصویر را جابه‌جا یا با لمس/چرخ ماوس بزرگ‌نمایی کنید.';
        show();
    };
    const baseName = (name, index) => {
        const base = name.replace(/\.[^.]+$/, '').replace(/[^a-zA-Z0-9\u0600-\u06ff_-]+/g, '-').replace(/^-+|-+$/g, '');
        return `${base || kind + '-image'}-${index + 1}.jpg`;
    };
    const replaceInputFiles = () => {
        const transfer = new DataTransfer();
        processed.forEach(file => transfer.items.add(file));
        input.files = transfer.files;
        input.setAttribute('data-crop-ready', 'true');
    };
    const confirm = () => {
        if (!cropper) return;
        const file = files[currentIndex];
        const data = cropper.getData(true);
        const scale = Math.min(1, target.width / data.width, target.height / data.height);
        const canvas = cropper.getCroppedCanvas({
            width: Math.max(1, Math.round(data.width * scale)),
            height: Math.max(1, Math.round(data.height * scale)),
            fillColor: '#ffffff',
            imageSmoothingEnabled: true,
            imageSmoothingQuality: 'high'
        });
        if (!canvas) return;
        canvas.toBlob(blob => {
            if (!blob) return;
            processed.push(new File([blob], baseName(file.name, currentIndex), {
                type: 'image/jpeg', lastModified: Date.now()
            }));
            currentIndex += 1;
            if (currentIndex < files.length) { openCurrent(); return; }
            replaceInputFiles();
            destroyCropper();
            hide();
            HTMLFormElement.prototype.submit.call(form);
        }, 'image/jpeg', 0.85);
    };
    const start = () => {
        if (!input.files.length) return;
        if (input.files.length > maxFiles) {
            input.setCustomValidity(`حداکثر ${maxFiles} تصویر انتخاب کنید.`);
            input.reportValidity();
            input.setCustomValidity('');
            return;
        }
        files = Array.from(input.files);
        processed = [];
        currentIndex = 0;
        input.removeAttribute('data-crop-ready');
        openCurrent();
    };

    input.addEventListener('change', start);
    form.addEventListener('submit', event => {
        if (!input.files.length || input.hasAttribute('data-crop-ready')) return;
        event.preventDefault();
        start();
    });
    document.querySelector('#image-editor-zoom-in').addEventListener('click', () => cropper && cropper.zoom(0.1));
    document.querySelector('#image-editor-zoom-out').addEventListener('click', () => cropper && cropper.zoom(-0.1));
    document.querySelector('#image-editor-reset').addEventListener('click', () => cropper && cropper.reset());
    document.querySelector('#image-editor-confirm').addEventListener('click', confirm);
    document.querySelector('#image-editor-cancel').addEventListener('click', cancel);
    document.querySelector('#image-editor-cancel-top').addEventListener('click', cancel);
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && !editor.hidden) cancel();
    });
})();
