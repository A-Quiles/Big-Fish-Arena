package com.aquiles.bigfisharena;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;

/**
 * Big Fish Arena: el juego (HTML) va dentro de la app y se muestra en un WebView.
 * La app añade lo que la web no puede hacer: el botón atrás, el bloqueo de versiones antiguas (UpdateGate),
 * los informes de fallos (CrashReporter) y, solo si se activan en gradle.properties, los anuncios (Ads).
 */
public class MainActivity extends ComponentActivity implements Ads.Host {
    private static final String TAG = "BigFishArena";
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/www/index.html";
    private static final int SEA = Color.rgb(6, 36, 58);

    private FrameLayout root;
    private WebView web;
    private Ads ads;
    private UpdateGate gate;
    private int rendererDeaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ads = new AdsImpl(this, this);
        CrashReporter.init(this, ads.enabled());

        root = new FrameLayout(this);
        root.setBackgroundColor(SEA);
        setContentView(root);

        // Pantalla completa, sin que el juego quede debajo del notch
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets c = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(c.left, c.top, c.right, c.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();

        createWebView();
        setupBackButton();

        gate = new UpdateGate(this, root, new UpdateGate.Listener() {
            @Override
            public void onBlocked() {
                pauseGame();
            }

            @Override
            public void onUnblocked() {
                resumeGame();
            }
        });
        gate.check(true);
        if (!gate.blocked()) ads.start();
    }

    // ---------- Juego ----------
    private void createWebView() {
        web = new WebView(this);
        root.addView(web, 0, new FrameLayout.LayoutParams(        // siempre debajo de la ventana de actualizar
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);           // aquí se guarda el progreso (localStorage)
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);                     // la interfaz del juego ya está pensada para móvil
        web.setBackgroundColor(SEA);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                openExternal(u);               // enlaces externos: al navegador
                return true;
            }

            // El motor del WebView se ha caído (falta de memoria...): sin esto se cerraría la app entera.
            // Se informa a Crashlytics y se vuelve a cargar el juego (el progreso está guardado).
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                String why = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? "crash=" + detail.didCrash() + ", prioridad=" + detail.rendererPriorityAtExit() : "?";
                CrashReporter.nonFatal(MainActivity.this, new IllegalStateException("WebView caído (" + why + ")"));
                if (view == web) {
                    root.removeView(view);
                    view.destroy();
                    web = null;
                    if (++rendererDeaths > 3 || isFinishing()) {
                        finish();
                    } else {
                        createWebView();
                        if (gate != null && gate.blocked()) pauseGame();
                    }
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "PezAndroid");
        web.loadUrl(START_URL);
    }

    @Override
    public void js(String code) {
        WebView w = web;
        if (w != null) w.post(() -> {
            if (web == w) w.evaluateJavascript(code, null);
        });
    }

    private void pauseGame() {
        if (web == null) return;
        js("window.__pezPause && window.__pezPause()");
        web.onPause();
        web.setVisibility(View.INVISIBLE);
    }

    private void resumeGame() {
        if (web == null) return;
        web.setVisibility(View.VISIBLE);
        web.onResume();
        js("window.__pezResume && window.__pezResume()");   // vuelve la música
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Con la ventana de actualizar no se puede volver al juego: la app pasa a segundo plano
                if (web == null || (gate != null && gate.blocked())) {
                    moveTaskToBack(true);
                    return;
                }
                // El juego decide: pausar, volver al menú... Si está en el menú, la app va a segundo plano.
                web.evaluateJavascript("!!(window.__pezBack && window.__pezBack())", used -> {
                    if (!"true".equals(used)) moveTaskToBack(true);
                });
            }
        });
    }

    @Override
    public void hideSystemBars() {
        runOnUiThread(() -> {
            WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            c.hide(WindowInsetsCompat.Type.systemBars());
        });
    }

    // ---------- Puente con el juego (window.PezAndroid) ----------
    // Estos nombres los usa el juego: R8 los conserva (proguard-rules.pro) aunque ofusque todo lo demás.
    private class Bridge {
        @JavascriptInterface
        public boolean adsEnabled() {
            return ads.enabled();
        }

        @JavascriptInterface
        public void showInterstitial() {
            ads.showInterstitial();
        }

        @JavascriptInterface
        public void showRewarded() {
            ads.showRewarded();
        }

        @JavascriptInterface
        public void showRewardedPearls() {
            ads.showRewardedPearls();
        }

        @JavascriptInterface
        public boolean privacyOptionsRequired() {
            return ads.privacyOptionsRequired();
        }

        @JavascriptInterface
        public void showPrivacyOptions() {
            ads.showPrivacyOptions();
        }

        @JavascriptInterface
        public void openUrl(String url) {
            if (url != null && url.startsWith("https://")) runOnUiThread(() -> openExternal(Uri.parse(url)));
        }

        @JavascriptInterface
        public void reportError(String msg, String stack) {
            CrashReporter.jsError(MainActivity.this, msg, stack);
        }
    }

    // ---------- Utilidades ----------
    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No hay navegador para abrir " + uri);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) {
            js("window.__pezPause && window.__pezPause()");
            web.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (gate != null) gate.check(false);
        if (web != null && (gate == null || !gate.blocked())) {
            web.onResume();
            js("window.__pezResume && window.__pezResume()");   // vuelve la música
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("PezAndroid");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
