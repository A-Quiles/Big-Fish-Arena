// Big Fish Arena: permite jugar sin conexión y recibir las versiones nuevas
const CACHE = 'big-fish-arena-849b2b12fc';
const CORE = ["./", "manifest.webmanifest", "icons/icon-192.png", "icons/icon-512.png", "icons/maskable-512.png", "fonts/fonts.css", "fonts/fredoka-latin-500-normal.woff2", "fonts/fredoka-latin-600-normal.woff2", "fonts/fredoka-latin-700-normal.woff2", "fonts/fredoka-latin-ext-500-normal.woff2", "fonts/fredoka-latin-ext-600-normal.woff2", "fonts/fredoka-latin-ext-700-normal.woff2", "fonts/nunito-cyrillic-700-normal.woff2", "fonts/nunito-cyrillic-800-normal.woff2", "fonts/nunito-cyrillic-900-normal.woff2", "fonts/nunito-cyrillic-ext-700-normal.woff2", "fonts/nunito-cyrillic-ext-800-normal.woff2", "fonts/nunito-cyrillic-ext-900-normal.woff2", "fonts/nunito-latin-700-normal.woff2", "fonts/nunito-latin-800-normal.woff2", "fonts/nunito-latin-900-normal.woff2", "fonts/nunito-latin-ext-700-normal.woff2", "fonts/nunito-latin-ext-800-normal.woff2", "fonts/nunito-latin-ext-900-normal.woff2"];
self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(CORE)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== location.origin) return;
  if (req.mode === 'navigate') {
    e.respondWith(fetch(req).then((r) => { const c = r.clone(); caches.open(CACHE).then((k) => k.put('./', c)); return r; }).catch(() => caches.match('./')));
    return;
  }
  e.respondWith(caches.match(req).then((hit) => hit || fetch(req).then((r) => { if (r.ok) { const c = r.clone(); caches.open(CACHE).then((k) => k.put(req, c)); } return r; })));
});
