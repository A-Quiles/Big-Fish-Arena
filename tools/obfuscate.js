// Ofusca el JavaScript del juego (lo usa tools/build_assets.py).
// Uso: node tools/obfuscate.js entrada.js salida.js ['{"opción": valor}']
//
// La salida es siempre la misma para la misma entrada (semilla fija): si alguna vez hay que descifrar un error
// de Crashlytics, basta con volver a generar el ofuscado de ese mismo commit con sourceMap activado.
'use strict';
const fs = require('fs');
const JavaScriptObfuscator = require('javascript-obfuscator');

const [, , input, output, extra] = process.argv;
if (!input || !output) {
  console.error('Uso: node tools/obfuscate.js entrada.js salida.js [opciones-json]');
  process.exit(2);
}

// Dominios donde puede funcionar el juego. Copiado en cualquier otra web, redirige a la oficial.
// Si algún día la web cambia de dirección (un dominio propio), hay que añadirla aquí.
const DOMAINS = ['big-fish-arena.vercel.app', 'appassets.androidplatform.net'];

const options = {
  target: 'browser',
  seed: 20261006,
  compact: true,
  simplify: true,

  // nombres sin sentido (todo el juego vive dentro de una función, así que se renombra todo)
  identifierNamesGenerator: 'mangled-shuffled',
  renameGlobals: false,
  renameProperties: false,              // las propiedades se guardan en localStorage y las usa la app: no tocarlas

  // Lo que NO se usa, medido (lógica + dibujo por fotograma, partida normal): la tabla de textos (stringArray) y
  // el código basura (deadCodeInjection, que la obliga) hacían el juego de 1,5 a 2 veces más lento; con
  // transformObjectKeys a la vez, aún peor. Las opciones de abajo cuestan muy poco y ya lo hacen muy difícil de leer.
  stringArray: false,
  deadCodeInjection: false,
  splitStrings: false,

  // la lógica, revuelta (con moderación: es un juego a 60 fotogramas por segundo)
  controlFlowFlattening: true,
  controlFlowFlatteningThreshold: 0.2,
  numbersToExpressions: true,           // los valores de ajuste del juego dejan de verse como números
  transformObjectKeys: true,

  // protecciones
  selfDefending: true,                  // si alguien lo «embellece» para leerlo, deja de funcionar
  disableConsoleOutput: true,
  domainLock: DOMAINS,
  domainLockRedirectUrl: 'https://big-fish-arena.vercel.app/',

  unicodeEscapeSequence: true,          // los textos del código, en \xNN: no se pueden buscar a simple vista
  sourceMap: false,
};

Object.assign(options, extra ? JSON.parse(extra) : {});
const code = fs.readFileSync(input, 'utf8');
const t0 = Date.now();
const result = JavaScriptObfuscator.obfuscate(code, options);
fs.writeFileSync(output, result.getObfuscatedCode());
if (options.sourceMap) fs.writeFileSync(output + '.map', result.getSourceMap());
console.log(`Ofuscado: ${(code.length / 1024).toFixed(0)} KB -> ${(result.getObfuscatedCode().length / 1024).toFixed(0)} KB en ${((Date.now() - t0) / 1000).toFixed(1)} s`);
