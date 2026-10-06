package com.aquiles.bigfisharena;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

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
 * Anuncios de AdMob (se compila con ADS_ENABLED=true en gradle.properties): un intersticial cuando
 * el juego lo pide (cada 5 partidas), anuncios con recompensa (cofre gratis, diamantes y perlas dobles)
 * y el consentimiento RGPD con UMP.
 */
final class AdsImpl implements Ads {
    private static final String TAG = "BigFishArena";

    private final ComponentActivity act;
    private final Host host;
    private ConsentInformation consent;
    private final String interstitialId;
    private InterstitialAd interstitial;
    private boolean adLoading;
    private final String rewardedId;
    private RewardedAd rewarded;
    private boolean rewardedLoading;
    private boolean rewardedWanted;             // el jugador lo ha pedido y aún se estaba cargando
    private final String rewardedPearlsId;
    private RewardedAd rewardedPearls;
    private boolean rewardedPearlsLoading;
    private boolean rewardedPearlsWanted;
    private final AtomicBoolean adsStarted = new AtomicBoolean(false);

    AdsImpl(ComponentActivity act, Host host) {
        this.act = act;
        this.host = host;
        interstitialId = readMeta("pez.INTERSTITIAL_ID");
        rewardedId = readMeta("pez.REWARDED_ID");
        rewardedPearlsId = readMeta("pez.REWARDED_PEARLS_ID");
    }

    @Override
    public boolean enabled() {
        return true;
    }

    // ---------- Consentimiento (RGPD) ----------
    @Override
    public void start() {
        consent = UserMessagingPlatform.getConsentInformation(act);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(act, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(act, formError -> {
                    if (formError != null) Log.w(TAG, "Consentimiento: " + formError.getMessage());
                    if (consent.canRequestAds()) startAds();
                    host.js("window.__pezConsentChanged && window.__pezConsentChanged()");
                }),
                error -> {
                    Log.w(TAG, "Consentimiento: " + error.getMessage());
                    if (consent.canRequestAds()) startAds();
                });
        if (consent.canRequestAds()) startAds();
    }

    @Override
    public boolean privacyOptionsRequired() {
        return consent != null && consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    @Override
    public void showPrivacyOptions() {
        act.runOnUiThread(() -> UserMessagingPlatform.showPrivacyOptionsForm(act, formError -> {
            if (consent != null && consent.canRequestAds()) startAds();
            host.js("window.__pezConsentChanged && window.__pezConsentChanged()");
        }));
    }

    // ---------- Anuncios ----------
    private void startAds() {
        if (adsStarted.getAndSet(true)) return;
        new Thread(() -> {
            MobileAds.initialize(act, status -> { });
            act.runOnUiThread(() -> {
                loadAd();
                loadRewarded();
                loadRewardedPearls();
            });
        }).start();
    }

    private void loadAd() {
        if (!adsStarted.get() || adLoading || interstitial != null || interstitialId == null) return;
        adLoading = true;
        InterstitialAd.load(act, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
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

    @Override
    public void showInterstitial() {
        act.runOnUiThread(this::showAd);
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
        interstitial.show(act);
    }

    // ---------- Anuncio con recompensa (cofre gratis / diamante) ----------
    private void loadRewarded() {
        if (!adsStarted.get() || rewardedLoading || rewarded != null || rewardedId == null) return;
        rewardedLoading = true;
        RewardedAd.load(act, rewardedId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
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

    @Override
    public void showRewarded() {
        act.runOnUiThread(this::showRewardedAd);
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
        rewarded.show(act, rewardItem -> earned[0] = true);
    }

    // ---------- Anuncio con recompensa (perlas dobles al final de partida) ----------
    private void loadRewardedPearls() {
        if (!adsStarted.get() || rewardedPearlsLoading || rewardedPearls != null || rewardedPearlsId == null) return;
        rewardedPearlsLoading = true;
        RewardedAd.load(act, rewardedPearlsId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
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
                    rewardFinished("noad");
                }
            }
        });
    }

    @Override
    public void showRewardedPearls() {
        act.runOnUiThread(this::showRewardedPearlsAd);
    }

    private void showRewardedPearlsAd() {
        if (rewardedPearls == null) {
            if (!adsStarted.get() || rewardedPearlsId == null) {
                rewardFinished("noad");
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
                rewardFinished(earned[0] ? "earned" : "closed");
                loadRewardedPearls();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                rewardedPearls = null;
                rewardFinished("noad");
                loadRewardedPearls();
            }
        });
        rewardedPearls.show(act, rewardItem -> earned[0] = true);
    }

    // ---------- Avisos al juego ----------
    private void rewardFinished(String status) {
        host.hideSystemBars();
        host.js("window.__pezRewardDone && window.__pezRewardDone('" + status + "')");
    }

    private void adFinished() {
        host.hideSystemBars();
        host.js("window.__pezAdDone && window.__pezAdDone()");
    }

    private String readMeta(String key) {
        try {
            ApplicationInfo ai = act.getPackageManager().getApplicationInfo(act.getPackageName(), PackageManager.GET_META_DATA);
            String v = ai.metaData != null ? ai.metaData.getString(key) : null;
            return v == null || v.isEmpty() ? null : v;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
