package com.aquiles.bigfisharena;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Big Fish Arena: el juego (HTML) va dentro de la app y se muestra en un WebView.
 * La app añade lo que la web no puede hacer: anuncios de AdMob (un intersticial
 * cuando el juego lo pide, cada 5 partidas, y un anuncio con recompensa para el
 * cofre gratis y los diamantes gratis), el consentimiento RGPD y el botón atrás.
 */
public class MainActivity extends ComponentActivity {
    private static final String TAG = "BigFishArena";
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/www/index.html";
    private static final int SEA = Color.rgb(6, 36, 58);

    private WebView web;
    private ConsentInformation consent;
    private String interstitialId;
    private InterstitialAd interstitial;
    private boolean adLoading;
    private String rewardedId;
    private RewardedAd rewarded;
    private boolean rewardedLoading;
    private boolean rewardedWanted;             // el jugador lo ha pedido y aún se estaba cargando
    private String rewardedPearlsId;
    private RewardedAd rewardedPearls;
    private boolean rewardedPearlsLoading;
    private boolean rewardedPearlsWanted;
    private final AtomicBoolean adsStarted = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        interstitialId = readMeta("pez.INTERSTITIAL_ID");
        rewardedId = readMeta("pez.REWARDED_ID");
        rewardedPearlsId = readMeta("pez.REWARDED_PEARLS_ID");

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SEA);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
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

        setupWebView();
        setupBackButton();
        gatherConsent();
    }

    // ---------- Juego ----------
    private void setupWebView() {
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
        });
        web.addJavascriptInterface(new Bridge(), "PezAndroid");
        web.loadUrl(START_URL);
    }

    private void js(String code) {
        if (web != null) web.post(() -> web.evaluateJavascript(code, null));
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // El juego decide: pausar, volver al menú... Si está en el menú, la app va a segundo plano.
                web.evaluateJavascript("!!(window.__pezBack && window.__pezBack())", used -> {
                    if (!"true".equals(used)) moveTaskToBack(true);
                });
            }
        });
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        c.hide(WindowInsetsCompat.Type.systemBars());
    }

    // ---------- Consentimiento (RGPD) ----------
    private void gatherConsent() {
        consent = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(this, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                    if (formError != null) Log.w(TAG, "Consentimiento: " + formError.getMessage());
                    if (consent.canRequestAds()) startAds();
                    js("window.__pezConsentChanged && window.__pezConsentChanged()");
                }),
                error -> {
                    Log.w(TAG, "Consentimiento: " + error.getMessage());
                    if (consent.canRequestAds()) startAds();
                });
        if (consent.canRequestAds()) startAds();
    }

    // ---------- Anuncios ----------
    private void startAds() {
        if (adsStarted.getAndSet(true)) return;
        new Thread(() -> {
            MobileAds.initialize(this, status -> { });
            runOnUiThread(() -> {
                loadAd();
                loadRewarded();
                loadRewardedPearls();
            });
        }).start();
    }

    private void loadAd() {
        if (!adsStarted.get() || adLoading || interstitial != null || interstitialId == null) return;
        adLoading = true;
        InterstitialAd.load(this, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitial = ad;
                adLoading = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                interstitial = null;
                adLoading = false;
                Log.w(TAG, "Anuncio no cargado: " + error.getMessage());
            }
        });
    }

    private void showAd() {
        if (interstitial == null) {             // no hay anuncio listo: el juego sigue sin esperar
            adFinished();
            loadAd();
            return;
        }
        interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitial = null;
                adFinished();
                loadAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                interstitial = null;
                adFinished();
                loadAd();
            }
        });
        interstitial.show(this);
    }

    // ---------- Anuncio con recompensa (cofre gratis / diamante) ----------
    private void loadRewarded() {
        if (!adsStarted.get() || rewardedLoading || rewarded != null || rewardedId == null) return;
        rewardedLoading = true;
        RewardedAd.load(this, rewardedId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewarded = ad;
                rewardedLoading = false;
                if (rewardedWanted) {
                    rewardedWanted = false;
                    showRewardedAd();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                rewarded = null;
                rewardedLoading = false;
                Log.w(TAG, "Anuncio con recompensa no cargado: " + error.getMessage());
                if (rewardedWanted) {
                    rewardedWanted = false;
                    rewardFinished("noad");
                }
            }
        });
    }

    private void showRewardedAd() {
        if (rewarded == null) {
            if (!adsStarted.get() || rewardedId == null) {
                rewardFinished("noad");
                return;
            }
            rewardedWanted = true;              // se muestra en cuanto termine de cargar
            loadRewarded();
            return;
        }
        final boolean[] earned = {false};
        rewarded.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewarded = null;
                rewardFinished(earned[0] ? "earned" : "closed");
                loadRewarded();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                rewarded = null;
                rewardFinished("noad");
                loadRewarded();
            }
        });
        rewarded.show(this, rewardItem -> earned[0] = true);
    }

    private void rewardFinished(String status) {
        hideSystemBars();
        js("window.__pezRewardDone && window.__pezRewardDone('" + status + "')");
    }

    // ---------- Anuncio con recompensa (perlas dobles al final de partida) ----------
    private void loadRewardedPearls() {
        if (!adsStarted.get() || rewardedPearlsLoading || rewardedPearls != null || rewardedPearlsId == null) return;
        rewardedPearlsLoading = true;
        RewardedAd.load(this, rewardedPearlsId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedPearls = ad;
                rewardedPearlsLoading = false;
                if (rewardedPearlsWanted) {
                    rewardedPearlsWanted = false;
                    showRewardedPearlsAd();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                rewardedPearls = null;
                rewardedPearlsLoading = false;
                Log.w(TAG, "Anuncio perlas dobles no cargado: " + error.getMessage());
                if (rewardedPearlsWanted) {
                    rewardedPearlsWanted = false;
                    rewardPearlsFinished("noad");
                }
            }
        });
    }

    private void showRewardedPearlsAd() {
        if (rewardedPearls == null) {
            if (!adsStarted.get() || rewardedPearlsId == null) {
                rewardPearlsFinished("noad");
                return;
            }
            rewardedPearlsWanted = true;
            loadRewardedPearls();
            return;
        }
        final boolean[] earned = {false};
        rewardedPearls.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedPearls = null;
                rewardPearlsFinished(earned[0] ? "earned" : "closed");
                loadRewardedPearls();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                rewardedPearls = null;
                rewardPearlsFinished("noad");
                loadRewardedPearls();
            }
        });
        rewardedPearls.show(this, rewardItem -> earned[0] = true);
    }

    private void rewardPearlsFinished(String status) {
        hideSystemBars();
        js("window.__pezRewardDone && window.__pezRewardDone('" + status + "')");
    }

    private void adFinished() {
        hideSystemBars();
        js("window.__pezAdDone && window.__pezAdDone()");
    }

    // ---------- Puente con el juego (window.PezAndroid) ----------
    private class Bridge {
        @JavascriptInterface
        public void showInterstitial() {
            runOnUiThread(MainActivity.this::showAd);
        }

        @JavascriptInterface
        public void showRewarded() {
            runOnUiThread(MainActivity.this::showRewardedAd);
        }

        @JavascriptInterface
        public void showRewardedPearls() {
            runOnUiThread(MainActivity.this::showRewardedPearlsAd);
        }

        @JavascriptInterface
        public boolean privacyOptionsRequired() {
            return consent != null && consent.getPrivacyOptionsRequirementStatus()
                    == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        }

        @JavascriptInterface
        public void showPrivacyOptions() {
            runOnUiThread(() -> UserMessagingPlatform.showPrivacyOptionsForm(MainActivity.this, formError -> {
                if (consent.canRequestAds()) startAds();
                js("window.__pezConsentChanged && window.__pezConsentChanged()");
            }));
        }

        @JavascriptInterface
        public void openUrl(String url) {
            if (url != null && url.startsWith("https://")) runOnUiThread(() -> openExternal(Uri.parse(url)));
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

    private String readMeta(String key) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String v = ai.metaData != null ? ai.metaData.getString(key) : null;
            return v == null || v.isEmpty() ? null : v;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        js("window.__pezPause && window.__pezPause()");
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        hideSystemBars();
        js("window.__pezResume && window.__pezResume()");   // vuelve la música
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
