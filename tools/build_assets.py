#!/usr/bin/env python3
"""Prepara Big Fish Arena a partir de game/big-fish-arena.html (la única fuente del juego).

Genera:
  - app/src/main/assets/www/  -> el juego dentro de la app Android
  - docs/                     -> versión web jugable + política de privacidad (GitHub Pages)

Uso: python3 tools/build_assets.py
"""
import hashlib, json, os, re, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'game', 'big-fish-arena.html')
APP = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'www')
DOCS = os.path.join(ROOT, 'docs')
FONTS = os.path.join(DOCS, 'fonts')
DESC = 'Eat smaller fish, grow bigger and don\'t get eaten. A brand-new sea every game.'

src = open(SRC, encoding='utf8').read()

# Fuentes incluidas (sin Google Fonts: funciona sin conexión y no envía datos a terceros)
# docs/fonts/fonts.css: latín, latín extendido (turco…) y cirílico (ruso); japonés, coreano y chino usan las fuentes del móvil
fonts_css = open(os.path.join(FONTS, 'fonts.css'), encoding='utf8').read().strip()
gf = re.search(r'<link rel="preconnect" href="https://fonts.googleapis.com">.*?display=swap" rel="stylesheet">\n', src, re.S)
assert gf, 'No encuentro el bloque de Google Fonts en el juego'
src = src.replace(gf.group(0), '<style>\n' + fonts_css + '\n</style>\n')

cut = re.search(r'</style>\n\n?<canvas', src)
assert cut, 'No encuentro el inicio del <body> del juego'
head, body = src[:cut.start() + len('</style>\n')], src[cut.start() + len('</style>\n'):]


def page(extra_head='', extra_body=''):
    return ('<!doctype html>\n<html lang="es">\n<head>\n<meta charset="utf-8">\n'
            '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n'
            f'<meta name="description" content="{DESC}">\n{extra_head}{head}</head>\n<body>\n{body}\n{extra_body}</body>\n</html>\n')


def copy_fonts(dst):
    os.makedirs(dst, exist_ok=True)
    for f in os.listdir(FONTS):
        shutil.copy(os.path.join(FONTS, f), os.path.join(dst, f))


# ---------- App Android ----------
if os.path.isdir(APP):
    shutil.rmtree(APP)
os.makedirs(APP)
open(os.path.join(APP, 'index.html'), 'w', encoding='utf8').write(page())
copy_fonts(os.path.join(APP, 'fonts'))

# ---------- Web (GitHub Pages) ----------
web = page(
    '<link rel="manifest" href="manifest.webmanifest">\n'
    '<link rel="icon" type="image/png" sizes="192x192" href="icons/icon-192.png">\n'
    '<link rel="apple-touch-icon" href="icons/icon-192.png">\n'
    '<meta name="mobile-web-app-capable" content="yes">\n',
    "<script>if ('serviceWorker' in navigator) addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));</script>\n")
open(os.path.join(DOCS, 'index.html'), 'w', encoding='utf8').write(web)
open(os.path.join(DOCS, '.nojekyll'), 'w').write('')
json.dump({
    'id': './', 'name': 'Big Fish Arena', 'short_name': 'Big Fish Arena', 'description': DESC, 'lang': 'en',
    'start_url': './', 'scope': './', 'display': 'fullscreen', 'orientation': 'any',
    'background_color': '#06243a', 'theme_color': '#06243a', 'categories': ['games'],
    'icons': [
        {'src': 'icons/icon-192.png', 'sizes': '192x192', 'type': 'image/png', 'purpose': 'any'},
        {'src': 'icons/icon-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'any'},
        {'src': 'icons/maskable-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'maskable'},
    ],
}, open(os.path.join(DOCS, 'manifest.webmanifest'), 'w', encoding='utf8'), ensure_ascii=False, indent=2)

core = ['./', 'manifest.webmanifest', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/maskable-512.png'] + ['fonts/' + f for f in sorted(os.listdir(FONTS))]
ver = hashlib.sha1(web.encode()).hexdigest()[:10]
open(os.path.join(DOCS, 'sw.js'), 'w', encoding='utf8').write(f'''// Big Fish Arena: permite jugar sin conexión y recibir las versiones nuevas
const CACHE = 'big-fish-arena-{ver}';
const CORE = {json.dumps(core)};
self.addEventListener('install', (e) => {{
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(CORE)).then(() => self.skipWaiting()));
}});
self.addEventListener('activate', (e) => {{
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
}});
self.addEventListener('fetch', (e) => {{
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== location.origin) return;
  if (req.mode === 'navigate') {{
    e.respondWith(fetch(req).then((r) => {{ const c = r.clone(); caches.open(CACHE).then((k) => k.put('./', c)); return r; }}).catch(() => caches.match('./')));
    return;
  }}
  e.respondWith(caches.match(req).then((hit) => hit || fetch(req).then((r) => {{ if (r.ok) {{ const c = r.clone(); caches.open(CACHE).then((k) => k.put(req, c)); }} return r; }})));
}});
''')
print('Juego preparado: app/src/main/assets/www y docs/ (versión', ver + ')')
