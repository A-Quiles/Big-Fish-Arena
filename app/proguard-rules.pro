# ===== Big Fish Arena: reglas de R8 (ofuscación y reducción del código Java) =====
# La versión de publicación ofusca todo lo posible: nombres de clases, métodos y campos sin sentido,
# todo junto en un único paquete y sin mensajes de registro. Solo se conserva lo imprescindible.

# --- Puente con el juego: el JavaScript llama a estos métodos por su nombre (window.PezAndroid.*) ---
-keepclassmembers class com.aquiles.bigfisharena.** {
    @android.webkit.JavascriptInterface <methods>;
}
# El WebView comprueba la anotación @JavascriptInterface en tiempo de ejecución
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# --- Crashlytics: trazas legibles en la consola de Firebase ---
# Se guardan números de línea; el plugin de Crashlytics sube el «mapping» para traducir los nombres ofuscados.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# --- Ofuscación agresiva ---
-repackageclasses ''
-allowaccessmodification
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# --- Fuera los mensajes de registro (y sus textos) ---
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static boolean isLoggable(java.lang.String, int);
}
