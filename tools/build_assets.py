#!/usr/bin/env python3
"""Prepara Big Fish Arena a partir de game/big-fish-arena.html (la única fuente del juego).

Genera:
  - app/src/main/assets/www/  -> el juego dentro de la app Android
  - docs/jugar/               -> versión web jugable (la portada docs/index.html es la página de promoción)

El JavaScript del juego sale ofuscado (tools/obfuscate.js, necesita Node: `npm ci --prefix tools` la primera vez)
y sin las herramientas de prueba (window.__pez). Las traducciones van aparte, como datos JSON.

Uso: python3 tools/build_assets.py
     BFA_DEBUG=1 python3 tools/build_assets.py   -> sin ofuscar y con window.__pez (para pruebas)
"""
import hashlib, json, os, re, shutil, subprocess, tempfile

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

# ---------- JavaScript de publicación ----------
DEBUG = os.environ.get('BFA_DEBUG') == '1'


def obfuscate(js):
    tool = os.path.join(ROOT, 'tools', 'obfuscate.js')
    if not os.path.isdir(os.path.join(ROOT, 'tools', 'node_modules', 'javascript-obfuscator')):
        raise SystemExit('Falta el ofuscador. Instálalo una vez con:  npm ci --prefix tools   (o usa BFA_DEBUG=1 para probar sin ofuscar)')
    with tempfile.TemporaryDirectory() as tmp:
        a, b = os.path.join(tmp, 'in.js'), os.path.join(tmp, 'out.js')
        open(a, 'w', encoding='utf8').write(js)
        subprocess.run(['node', tool, a, b], check=True)
        return open(b, encoding='utf8').read()


sm = re.search(r'<script>\n(.*?)\n</script>', body, re.S)
assert sm and body.count('<script>') == 1, 'El juego debería tener un único <script>'
js = sm.group(1)
# Traducciones: fuera del código, como datos JSON (más rápido de cargar y no hace falta ofuscarlas)
tm = re.search(r'^const I18N = (\{.*\});$', js, re.M)
assert tm, 'No encuentro las traducciones (const I18N = {...};)'
js = js.replace(tm.group(0), "const I18N = JSON.parse(document.getElementById('i18n').textContent);")
i18n_block = '<script type="application/json" id="i18n">' + tm.group(1).replace('<', '\\u003c') + '</script>\n'
if not DEBUG:
    pm = re.search(r'^window\.__pez = \{.*\};$', js, re.M)
    assert pm, 'No encuentro window.__pez (herramientas de prueba)'
    js = js.replace(pm.group(0), '')
    js = obfuscate(js)
body = body[:sm.start()] + i18n_block + '<script>\n' + js + '\n</script>' + body[sm.end():]


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

# ---------- Web: el juego jugable en https://big-fish-arena.vercel.app/jugar/ ----------
# La portada (docs/index.html + docs/assets/) es la página de promoción y no la toca este script.
WEB = os.path.join(DOCS, 'jugar')
if os.path.isdir(WEB):
    shutil.rmtree(WEB)
os.makedirs(WEB)
copy_fonts(os.path.join(WEB, 'fonts'))
web = page(
    '<link rel="manifest" href="manifest.webmanifest">\n'
    '<link rel="icon" type="image/png" sizes="192x192" href="../icons/icon-192.png">\n'
    '<link rel="apple-touch-icon" href="../icons/icon-192.png">\n'
    '<meta name="mobile-web-app-capable" content="yes">\n',
    "<script>if ('serviceWorker' in navigator) addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));</script>\n")
open(os.path.join(WEB, 'index.html'), 'w', encoding='utf8').write(web)
open(os.path.join(DOCS, '.nojekyll'), 'w').write('')
json.dump({
    'id': './', 'name': 'Big Fish Arena', 'short_name': 'Big Fish Arena', 'description': DESC, 'lang': 'en',
    'start_url': './', 'scope': './', 'display': 'fullscreen', 'orientation': 'any',
    'background_color': '#06243a', 'theme_color': '#06243a', 'categories': ['games'],
    'icons': [
        {'src': '../icons/icon-192.png', 'sizes': '192x192', 'type': 'image/png', 'purpose': 'any'},
        {'src': '../icons/icon-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'any'},
        {'src': '../icons/maskable-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'maskable'},
    ],
}, open(os.path.join(WEB, 'manifest.webmanifest'), 'w', encoding='utf8'), ensure_ascii=False, indent=2)

core = ['./', 'manifest.webmanifest', '../icons/icon-192.png', '../icons/icon-512.png', '../icons/maskable-512.png'] + ['fonts/' + f for f in sorted(os.listdir(FONTS))]
ver = hashlib.sha1(web.encode()).hexdigest()[:10]
open(os.path.join(WEB, 'sw.js'), 'w', encoding='utf8').write(f'''// Big Fish Arena: permite jugar sin conexión y recibir las versiones nuevas
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

# Antes el juego estaba en la portada: quien lo abrió tiene un service worker en «/» que seguiría enseñando el juego.
# Este sw.js de la raíz se borra a sí mismo y recarga, para que vean la página de promoción.
open(os.path.join(DOCS, 'sw.js'), 'w', encoding='utf8').write('''// Retirado: el juego se mueve a /jugar/. Este service worker se elimina solo y recarga la página.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.map((k) => caches.delete(k))))
    .then(() => self.registration.unregister())
    .then(() => self.clients.matchAll({ type: 'window' }))
    .then((cs) => cs.forEach((c) => c.navigate(c.url))));
});
''')
old_manifest = os.path.join(DOCS, 'manifest.webmanifest')
if os.path.exists(old_manifest):
    os.remove(old_manifest)
print('Juego preparado: app/src/main/assets/www y docs/jugar/ (versión', ver + ')')
