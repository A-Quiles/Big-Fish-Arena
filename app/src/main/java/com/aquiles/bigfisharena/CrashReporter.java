package com.aquiles.bigfisharena;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.webkit.WebViewCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Informes de fallos con Firebase Crashlytics.
 * Los cierres de la app (Java) se envían solos. Además se envían, como fallos «no fatales»,
 * los errores del juego (JavaScript) y las caídas del motor del WebView.
 * Si la app se compila sin google-services.json, Firebase no arranca y todo esto no hace nada.
 */
final class CrashReporter {
    private static final Pattern JS_FRAME = Pattern.compile("^\\s*at (?:(.*?) \\()?(.*?):(\\d+):(\\d+)\\)?\\s*$");
    private static Boolean ready;

    private CrashReporter() {
    }

    private static boolean ready(Context c) {
        if (ready == null) {
            try {
                ready = !FirebaseApp.getApps(c.getApplicationContext()).isEmpty();
            } catch (Throwable t) {
                ready = false;
            }
        }
        return ready;
    }

    static void init(Context c, boolean adsEnabled) {
        if (!ready(c)) return;
        try {
            FirebaseCrashlytics cr = FirebaseCrashlytics.getInstance();
            cr.setCustomKey("ads", adsEnabled);
            PackageInfo wv = WebViewCompat.getCurrentWebViewPackage(c);
            if (wv != null) cr.setCustomKey("webview", wv.packageName + " " + wv.versionName);
        } catch (Throwable ignored) {
        }
    }

    static void log(Context c, String msg) {
        if (!ready(c)) return;
        try {
            FirebaseCrashlytics.getInstance().log(msg);
        } catch (Throwable ignored) {
        }
    }

    static void nonFatal(Context c, Throwable t) {
        if (!ready(c)) return;
        try {
            FirebaseCrashlytics.getInstance().recordException(t);
        } catch (Throwable ignored) {
        }
    }

    /** Error del juego: se agrupa en Crashlytics por el punto del código donde ocurrió. */
    static void jsError(Context c, String msg, String stack) {
        if (!ready(c)) return;
        JsError e = new JsError(msg == null || msg.isEmpty() ? "Error de JavaScript" : msg);
        List<StackTraceElement> frames = new ArrayList<>();
        if (stack != null) {
            for (String line : stack.split("\n")) {
                Matcher m = JS_FRAME.matcher(line);
                if (!m.matches()) continue;
                String fn = m.group(1) == null || m.group(1).isEmpty() ? "<anónima>" : m.group(1);
                String url = m.group(2);
                String file = url.substring(url.lastIndexOf('/') + 1);
                try {
                    frames.add(new StackTraceElement("js", fn, file + ":" + m.group(3), Integer.parseInt(m.group(4))));
                } catch (NumberFormatException ignored) {
                }
                if (frames.size() >= 40) break;
            }
        }
        if (frames.isEmpty()) frames.add(new StackTraceElement("js", "?", "index.html", 0));
        e.setStackTrace(frames.toArray(new StackTraceElement[0]));
        try {
            FirebaseCrashlytics cr = FirebaseCrashlytics.getInstance();
            if (stack != null) cr.setCustomKey("js_stack", stack.length() > 1000 ? stack.substring(0, 1000) : stack);
            cr.recordException(e);
        } catch (Throwable ignored) {
        }
    }

    /** Error del juego (JavaScript) enviado como fallo no fatal. */
    static final class JsError extends RuntimeException {
        JsError(String msg) {
            super(msg);
        }
    }
}
