# Big Fish Arena 🐟

Juego de peces para móvil: come, crece y que no te coman.

- **Jugar en el navegador:** https://a-quiles.github.io/BigFishArena/
- **Política de privacidad:** https://a-quiles.github.io/BigFishArena/privacidad.html

## Cómo está hecho

| Carpeta | Qué hay |
|---|---|
| `game/big-fish-arena.html` | **El juego entero** (HTML + CSS + JS en un solo archivo). Es lo único que hay que tocar para cambiar el juego. |
| `app/` | App Android (Java): muestra el juego en un WebView, pone los anuncios de AdMob y pide el consentimiento RGPD. |
| `docs/` | Versión web jugable y política de privacidad. Si este repositorio es privado, la política se publica desde el repositorio público `a-quiles.github.io` (carpeta `BigFishArena/`), junto con `app-ads.txt`. |
| `store/` | Icono, gráfico destacado, capturas y textos para la ficha de Google Play. |
| `tools/build_assets.py` | Copia el juego a `app/` y `docs/`. |
| `.github/workflows/android.yml` | Compila la app en la nube en cada cambio. |

## Idiomas

El juego está en 12 idiomas: español, inglés, portugués (Brasil), francés, alemán, italiano, ruso, turco, indonesio, japonés, coreano y chino simplificado.

- La primera vez usa el idioma del móvil (si no está entre esos, inglés). Se cambia en **Ajustes → Idioma**.
- Los textos se escriben en español en `game/big-fish-arena.html` y se traducen al mostrarse. Las traducciones están dentro del mismo archivo, en `const I18N`, con el texto español como clave.
- Si añades o cambias un texto en español, hay que añadir su traducción en cada idioma (pídemelo y lo hago); mientras tanto ese texto sale en español.
- Las fuentes incluyen letras latinas (también turco) y cirílicas; japonés, coreano y chino usan las fuentes del móvil.

## Modos de juego

- **Clásico:** un pez que come, crece y huye.
- **Banco de peces** (se desbloquea al llegar a talla 6 en Clásico): manejas un banco de hasta 50 sardinas. Crece comiendo krill y peces pequeños o juntando sardinas sueltas. Los depredadores hacen pasadas contra el banco: el que ataca se marca en rojo con «!», acelera al final y los grandes se tragan varias sardinas de un bocado. «Atacar» hace que todas muerdan a la vez; «Bola» aprieta el banco 3 s y nadie os puede morder. El récord es el banco más grande.

En los dos modos:

- **Pescadores:** de vez en cuando llega un barco con la bandera de buceo: es el aviso de que hay buceadores cerca (si estás hondo y no se ve, sale la etiqueta «Buceadores cerca»). Se tiran al agua uno o dos buceadores con fusil de arpón, que solo apunta hacia delante (unos 20° arriba o abajo), así que se ponen a tu altura para disparar. Antes de disparar apuntan con una línea roja; se esquiva subiendo o bajando, y a mordiscos se les espanta de vuelta al barco. El barco se va cuando vuelven. En el modo banco solo aparecen cuando el banco tiene 10 sardinas o más.
- **Mar sin bordes:** el mapa se repite en horizontal; si sales por un lado, entras por el otro.
- **Velocidad relativa a ti:** tu pez gana velocidad al crecer (siendo grande te sigues notando ágil) y los demás se mueven en relación a tu tamaño: los mucho más grandes que tú, más lentos (a 2× tu tamaño van a un 72 %, a 3× a un 66 %); los más pequeños, algo más rápidos. La proporción es la misma seas de la talla que seas. (En el modo banco se mantiene la velocidad de siempre.)
- **Control preciso:** el pez frena en cuanto sueltas el control, sin apenas deslizarse (salvo durante el mordisco, el acelerón o una embestida).
- **Dificultad** (botón 🎚️ del menú, junto al récord): Fácil, Normal (el equilibrio original), Difícil o A tu medida, con 5 niveles para la agresividad de los depredadores, los peces gigantes, el daño, la velocidad y la frecuencia de pescadores. Las perlas se multiplican: Fácil ×0,5, Normal ×1, Difícil ×1,5 y, a tu medida, de ×0,25 a ×3 (cada ajuste suma según lo que complica la partida). Cada dificultad guarda su propio récord.

## El mar

- Cada especie vive en sus zonas y a su profundidad (como en la vida real): sardinas y peces voladores cerca de la superficie, pulpos, rayas, meros, anguilas y rapes pegados al fondo, pez linterna en las aguas oscuras… Al pasear, cada pez vuelve a su franja.
- Cangrejos: caminan de lado por el fondo de las zonas poco profundas (arrecife, bosque de algas, laguna y algo en mar abierto). Los pequeños son presa; los grandes pellizcan.
- Cangrejo araña (como el cangrejo gigante japonés): más grande y con patas larguísimas; vive en el fondo del abismo y de la fosa abisal.
- Como mucho hay 1 cangrejo de cada tipo a la vez cerca del jugador.
- Al morder, los cangrejos se inclinan hacia la presa y lanzan una pinza (cangrejo) o la pata delantera (cangrejo araña); se dan la vuelta sin aplastarse.

## Mares

Se eligen en el menú con el botón del mar (debajo de «Jugar»). Cada uno tiene su agua, su luz, sus zonas, sus especies y su propio récord.

- **Mar templado** (gratis): arrecifes, algas, lagunas y el abismo con lava.
- **Antártida** (1500 perlas o 60 diamantes): pez de hielo, austromerluza, pingüinos y orcas. En la superficie flotan témpanos con iglús y pingüinos. Son icebergs de verdad, de tamaños muy distintos, con casi todo el hielo bajo el agua (quillas de hasta ~90 m que hay que rodear). Si tu pez cae encima de uno, se queda varado (y resbala por las pendientes) y tiene 5 s para saltar por el borde al agua o muere. Los osos polares vigilan desde los témpanos y se lanzan al agua a por los peces que nadan cerca de la superficie. Bajo el agua hay bloques de hielo que hacen de obstáculo.
- **Mar tóxico** (2500 perlas o 100 diamantes): un mapa más pequeño y sucio. Si tu pez se traga una botella, una bolsa o una lata, encoge (y si ya es mínimo, pierde vida). Las tuberías del fondo vierten veneno y los bidones radiactivos que gotean queman poco a poco a quien se acerque (unos 20 s en el chorro y 30 s junto a un bidón para matarte). Antes de entrar, una alarma amarilla avisa: «Zona contaminada cerca». Aquí viven peces mutantes de tres ojos.
- **Cuevas:** en el fondo de cada mar hay 2 o 3 cuevas, generadas al azar en cada partida: pequeñas (unos 15 túneles), medianas (25–35) o grandes y anchas (40–60), con varias entradas, pasos estrechos, galerías a distintos niveles, salas, bucles y callejones sin salida. Desde fuera solo se ve la entrada: una roca con una abertura en arco, con estalactitas (y musgo, nieve o babas verdes según el mar). Dentro, los túneles se van descubriendo según los recorres; están muy oscuras y viven peces dentro (a veces una anguila en las grandes). **Recompensas:** perlas sueltas por los túneles y tesoros en las salas que valen más cuanto más lejos de la entrada (de ×1,4 a ×3 un cofre normal); la sala más lejana de las cuevas grandes guarda un cofre de oro (y a veces un diamante). Lo que está bajo el fondo (túneles, peces, cofres) solo se ve desde dentro y una vez descubierto; la vista de dentro solo se activa cuando has entrado de verdad, así que si no cabes no ves nada. **Tamaño:** si eres demasiado grande no puedes entrar (te lo avisa al llegar a la puerta). Si creces dentro y un túnel se te queda pequeño, te quedas atascado: avanzas muy despacio y la roca te hace daño hasta que salgas a un túnel donde quepas (y si tardas demasiado, mueres atrapado). Todo se calcula al crear el mar, así que apenas cuesta rendimiento. **Guardianes:** la sala de cada tesoro la vigila un pez algo más grande que tú (borde naranja: no puede tragarte, pero da pelea) que aparece al entrar en la cueva con tu tamaño de ese momento; el cofre está cerrado con candado hasta que lo vences. **Peligros:** morenas escondidas en grietas (se les ven los ojos) que muerden al pasar y derrumbes que cortan un túnel unos segundos. **Especies de cueva:** pez ciego (sin ojos) y pez luciérnaga (alumbra a su alrededor), en la Guía. **Mapa:** dentro, el minimapa enseña la cueva: lo explorado, las entradas y los tesoros (naranja si aún tienen guardián). Explorar una cueva entera da premio. **Mejora «Linterna»** (4 niveles): ves más lejos dentro. **Misiones** de tesoros de cueva, guardianes y cuevas exploradas. Dentro hay eco y goteo.
- **Modo demo (desactivado):** `MAPS_FREE = true` abre todos los mares sin pagar y `PEARLS_FREE = true` da perlas ilimitadas. Ahora los dos están en `false` (juego normal). Una partida guardada que venga de la demo vuelve a 0 perlas y al mar templado la primera vez que se abre.

## Menú

- El menú principal solo tiene lo esencial: perlas, el engranaje de **Ajustes**, tu pez, el modo, **Jugar**, récord y dificultad, y Tienda, Mejoras, Misiones y Guía.
- **Tu pez:** las flechas (o deslizar el dedo) cambian entre tus especies sin salir del menú. Al tocarlo se abre «Tu pez», donde eliges especie y aspecto y desbloqueas especies nuevas (sustituye al antiguo Acuario).
- **Ajustes (⚙️):** sonido, música, vibración, calidad gráfica, «Cómo se juega» y, en la app, la privacidad y los anuncios.

## Gráficos

- Los peces se «hornean» en sprites con volumen (degradado, brillo, luz de contorno, silueta gruesa, aletas translúcidas y escamas) y se dibujan en tiras que siguen la onda del nado. Los ojos miran a la presa o al peligro, parpadean y cambian con el estado (tranquilo, cazando, asustado, comiendo, aturdido, herido).
- Animaciones de mordisco, trago, acelerón, golpe, subir de talla y muerte, con parada de impacto, temblor de cámara según el daño y vibración (se puede apagar en Ajustes).
- Al cambiar de sentido, los peces (y las gaviotas y los buceadores) no se voltean de golpe: giran en unas décimas de segundo, con un coletazo; los grandes, algo más despacio.
- El fondo va por capas (lejos, medio, juego y primer plano) con color, luz y niebla propios de cada zona.
- Calidad gráfica en Ajustes: Auto, Alta, Media o Baja. En Auto el juego mide los primeros segundos de cada partida y, si el móvil no llega a unos 46 fps, usa una calidad más baja **a partir de la partida siguiente** (nunca cambia a mitad de partida).
- El fondo lejano (agua, relieve del fondo, cielo, rayos de luz y nieve marina) se pinta a menos resolución y se escala.
- En pausa y en la pantalla de resultados el mar se pinta una sola vez.

## Rendimiento en el móvil

- **Resolución fija**: ya no hay resolución automática; el lienzo tiene siempre la resolución de su calidad. Los cambios de tamaño de la ventana se aplican al empezar un fotograma, antes de pintarlo.
- **Tope de 60 fps**: el juego mide el refresco de la pantalla y en pantallas de 120 Hz pinta uno de cada dos refrescos (en 144 Hz, 72 fps; en 90 Hz se deja a 90). Menos trabajo, menos calor y sin bajones al calentarse el móvil.
- **Sprites sin tirones**: el horneado se hace por pasos y se reparte entre fotogramas con un presupuesto de tiempo (2,5 ms por fotograma; 6 ms durante el primer segundo de cada partida). Los peces que van a entrar en pantalla se piden por adelantado; mientras tanto se usa el mismo pez a otra resolución o con la boca cerrada.
- **Peces con menos órdenes de dibujo**: cada pez se coloca con una sola orden (la matriz se calcula a mano), los ojos son imágenes pequeñas ya preparadas (por tamaño, expresión, mirada y parpadeo) y los peces pequeños usan menos tiras. Con el banco de sardinas, las órdenes de dibujo por fotograma bajaron de ~3.300 a ~1.200.
- **Marcador en caché**: fondo y marco de las barras, botones de pausa, morder y habilidad, y el marco del minimapa son imágenes; solo se pinta lo que cambia.
- **Memoria**: temporizadores de los peces escritos uno a uno, listas que se compactan sobre sí mismas en vez de rehacerse, partículas recicladas y peces que nacen con todas sus propiedades en el mismo orden.
- Sin `ctx.filter` durante la partida (en el móvil es lentísimo): el destello del buceador al recibir un mordisco es un brillo aditivo.

## Botones y mapa

En **Ajustes → Botones y mapa** se arrastran el botón de morder (o Atacar en el banco), el de la habilidad (o Bola) y el minimapa, y se cambia su tamaño: los botones entre el 70 % y el 140 % y el minimapa entre el 70 % y el 160 %. Se guarda aparte para el móvil en vertical y en horizontal, y «Restablecer» vuelve a la posición de siempre. El botón de morder funciona aunque se ponga en la mitad izquierda de la pantalla.

## Final de la partida

Las perlas de la partida se guardan en un cofre con una animación: una fila por cada origen (por tus puntos, con imán o dificultad si los hay; cofres hundidos; cuevas; misiones) que cuenta hacia arriba mientras las perlas vuelan al cofre y sube tu total. Tocar el cuadro lo termina al momento.

**Primera partida del día:** las perlas de tus puntos se duplican.

**Economía de perlas.** Los puntos se convierten con rendimientos decrecientes: 1 perla cada 8 puntos hasta 800 puntos, 1 cada 16 hasta 3200 y 1 cada 32 a partir de ahí (300 puntos → 37 perlas, 3200 → 250, 6000 → 337, 10 000 → 462; antes eran 37, 400, 750 y 1250). Los tesoros de cueva valen de ×1,4 a ×3 un cofre normal (+1,5 el de oro de las cuevas grandes), con un 3 % de diamante (20 % el de oro); las perlas sueltas valen 1 o 2 y explorar una cueva entera da 15, 30 o 50.

## Música

Cada zona tiene su propia música de fondo, generada en el propio juego con Web Audio (sin archivos ni derechos de autor). Se activa o desactiva en Ajustes.

## Anuncios

- Un anuncio intersticial **cada 5 partidas**, al salir de la pantalla de resultados. Nunca durante la partida.
- **Perlas dobles** al final de cada partida viendo un anuncio con recompensa (voluntario, una vez por partida; duplica las perlas de puntos, cofres y cuevas, no las de misiones). Si lo ves, esa vez no sale el intersticial.
- **Cofre dorado gratis** viendo un anuncio con recompensa (voluntario), uno al día.
- **Diamante gratis** viendo un anuncio con recompensa: 1 💎 por anuncio, hasta 5 al día (Tienda → Diamantes).
- Los IDs de AdMob están en `gradle.properties`. Ahora son los **IDs de prueba** de Google: cámbialos por los tuyos antes de publicar.
- El consentimiento (RGPD) lo gestiona el SDK de Google (UMP) con el mensaje que configures en AdMob → Privacidad y mensajes.
- En el navegador no hay anuncios.

## Diamantes

`const DIAMONDS = true;` en `game/big-fish-arena.html` (con `false` se oculta todo lo de diamantes).

**Cómo se consiguen**
| Fuente | Cantidad |
|---|---|
| Anuncio con recompensa (Tienda) | 1 💎, hasta 5 al día |
| Misión difícil | 5 💎 cada vez (al cumplirla sale otra) |
| Cofre dorado (400 perlas) | 4 % de probabilidad de 3 💎 |
| Cofres hundidos | 1 % de probabilidad de 1 💎 |
| Escondido en lo más hondo del mar | 1 💎 (aparece en ~12 % de las partidas) |

**En qué se gastan**
- Aspectos exclusivos, cada uno para un pez: Hielo 40 · Koi y Tigre 50 · Tesoro real y Fantasma 60 · Lava 80 · Eléctrico 90 · Dragón 100 · Robot 110 · Holográfico 120 · Tóxico y Galaxia 150. Cada uno tiene su propio dibujo (cristales, nebulosas, grietas de magma, escamas de oro, rayas de tigre, rayos, placas de metal, escamas iridiscentes…), brillo y rastro de partículas.
- Mares: Antártida por 60 💎 (o 1500 perlas) y Mar tóxico por 100 💎 (o 2500 perlas).

**Sin compras con dinero real**
- `const PAID_GEMS = false;` en `game/big-fish-arena.html`: no se venden diamantes; solo se ganan jugando y con anuncios.
- La versión con pagos de Google Play (packs de 0,99 € a 19,99 € y el código de facturación en `MainActivity.java`) está en el historial de git, en el commit «Economía real: diamantes con Google Play Billing…». Para recuperarla: `PAID_GEMS = true`, volver a poner ese `MainActivity.java` y la dependencia `com.android.billingclient:billing:8.0.0`, y crear los productos `diamantes_50`, `diamantes_280`, `diamantes_600` y `diamantes_1300` en Play Console.

## Compilar

Cada `push` a `main` compila en GitHub Actions y deja en **Actions → la última ejecución → Artifacts**:
- `app-release.aab` → para subir a Google Play
- `app-release.apk` → para instalar a mano en tu móvil y probar

La firma usa estos secretos del repositorio (Settings → Secrets and variables → Actions):
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

En local (con Android Studio o Gradle 9.4 y JDK 21): `python3 tools/build_assets.py` y luego `gradle :app:assembleRelease`.
