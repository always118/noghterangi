const fs = require('fs');

const source = JSON.parse(fs.readFileSync('noghterangi-export/posts.json', 'utf8'));
const catalog = JSON.parse(fs.readFileSync('dev-data/category-catalog.json', 'utf8'));
const persianDigits = '۰۱۲۳۴۵۶۷۸۹';
const arabicDigits = '٠١٢٣٤٥٦٧٨٩';

function latin(value) {
  return String(value).replace(/[۰-۹]/g, c => String(persianDigits.indexOf(c)))
    .replace(/[٠-٩]/g, c => String(arabicDigits.indexOf(c)));
}

function clean(value) {
  return value.replace(/\r/g, '').replace(/[ \t]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
}

function title(text) {
  const line = clean(text).split('\n').map(s => s.trim()).find(Boolean) || 'محصول کانال';
  return line.replace(/^[^\p{L}\p{N}]+/u, '').replace(/[؛،,:：]+$/, '').slice(0, 160) || 'محصول کانال';
}

function category(text) {
  const value = text.toLowerCase();
  if (/بازی|مکعب|کوبه|مونتی|گارنت|زنبور|کارینو|ست آشپزی|ست دکتری|ست ابزار|ست آرایشگری/.test(value)) return 'board-games';
  if (/کتاب|دفتر|کاغذ|جلد چسبی/.test(value)) return /کتاب/.test(value) ? 'childrens-books' : 'exercise-books';
  if (/خمیربازی|خمیر بازی|گواش|آبرنگ|رنگ انگشتی|پاستل|ماژیک رنگ/.test(value)) return 'art-creativity';
  if (/جامدادی|کوله‌پشتی|کوله پشتی/.test(value)) return 'bags-pencil-cases';
  if (/قمقمه|بطری/.test(value)) return 'bottles-flasks';
  if (/پک آموزش|پک کلاس|کارت الفبا|آموزش ریاضی/.test(value)) return 'educational-supplies';
  return 'stationery';
}

function productType(slug) {
  if (slug === 'board-games') return 'BOARD_GAME';
  if (slug === 'childrens-books') return 'BOOK';
  if (slug === 'educational-supplies' || slug === 'art-creativity') return 'EDUCATIONAL';
  return 'STATIONERY';
}

function priceInfo(text) {
  const value = latin(text).replace(/[،]/g, ',').replace(/[٬]/g, ',');
  const matches = [...value.matchAll(/(?:قیمت(?:\s+[^:：\n]{0,25})?\s*[:：]?\s*)([0-9][0-9,.]*)\s*(هزار\s*تومان|هزارتومان|تومان|تومن|ریال)?/gi)];
  const all = matches.length ? matches : [...value.matchAll(/([0-9][0-9,.]*)\s*(هزار\s*تومان|هزارتومان|تومان|تومن|ریال)/gi)];
  if (!all.length) return { priceToman: 0, priceReviewRequired: true, priceSource: null };
  const raw = all[0][1].replace(/[,.]/g, '');
  let price = Number(raw);
  const unit = (all[0][2] || '').replace(/\s/g, '').toLowerCase();
  if (unit === 'هزارتومان' || unit === 'تومان' || unit === 'تومن') {
    if (unit === 'هزارتومان' || price < 10000) price *= 1000;
  } else if (unit === 'ریال') {
    price = Math.round(price / 10);
  }
  const ambiguous = all.length > 1 || /قیمت\s*:\s*3[0-9]{2}\s*تومان/.test(value);
  return { priceToman: Number.isSafeInteger(price) ? price : 0, priceReviewRequired: ambiguous, priceSource: all[0][0] };
}

const anchors = source.filter(post => post.text && post.text.trim() && post.media && post.media.length);
const products = anchors.map((post, index) => {
  const next = anchors[index + 1];
  const collected = [];
  for (const candidate of source) {
    if (candidate.messageId < post.messageId || (next && candidate.messageId >= next.messageId)) continue;
    if (candidate.media) collected.push(...candidate.media);
  }
  const name = title(post.text);
  const categorySlug = category(post.text);
  const pricing = priceInfo(post.text);
  const unavailable = /اتمام\s*موجودی|ناموجود/.test(post.text);
  return {
    sourcePostId: post.messageId,
    sourceDate: new Date(post.date * 1000).toISOString(),
    name,
    slug: `channel-post-${post.messageId}`,
    sku: `CHANNEL-${post.messageId}`,
    categorySlug,
    type: productType(categorySlug),
    brand: null,
    shortDescription: clean(post.text).replace(/\n/g, ' ').slice(0, 500),
    description: clean(post.text),
    priceToman: pricing.priceToman,
    priceReviewRequired: pricing.priceReviewRequired || pricing.priceToman === 0,
    priceSource: pricing.priceSource,
    stockQuantity: 0,
    active: !unavailable,
    featured: false,
    displayOrder: index,
    images: [...new Set(collected)],
    sourceTextContainsOutOfStock: unavailable
  };
});

const result = {
  schemaVersion: 1,
  source: 'noghterangi-export/posts.json',
  mediaRoot: 'noghterangi-export',
  generatedAt: new Date().toISOString(),
  reviewNotes: [
    'priceToman is inferred from the caption and must be reviewed before sales.',
    'stockQuantity is intentionally zero because the channel export does not prove current inventory.',
    'Existing products are not overwritten by the importer unless deleteExisting is explicitly requested.'
  ],
  categories: catalog.roots.flatMap(root => [
    { name: root.name, slug: root.slug, parentSlug: null },
    ...root.children.map(child => ({ name: child.name, slug: child.slug, parentSlug: root.slug }))
  ]),
  products
};

fs.writeFileSync('dev-data/channel-products.json', JSON.stringify(result, null, 2) + '\n');
console.log(`Wrote ${products.length} channel products and ${products.reduce((n, p) => n + p.images.length, 0)} media references.`);
