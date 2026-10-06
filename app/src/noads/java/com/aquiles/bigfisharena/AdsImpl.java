package com.aquiles.bigfisharena;

import androidx.activity.ComponentActivity;

/**
 * Versión sin anuncios (ADS_ENABLED=false en gradle.properties): no hay AdMob ni consentimiento de anuncios
 * y el SDK de anuncios ni siquiera se incluye en la app. El juego oculta todo lo relacionado con anuncios.
 * Si aun así se pidiera un anuncio, se responde al momento «no hay anuncio» para que el juego nunca se quede esperando.
 */
final class AdsImpl implements Ads {
    private final Host host;

    AdsImpl(ComponentActivity act, Host host) {
        this.host = host;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void showInterstitial() {
        host.js("window.__pezAdDone && window.__pezAdDone()");
    }

    @Override
    public void showRewarded() {
        host.js("window.__pezRewardDone && window.__pezRewardDone('noad')");
    }

    @Override
    public void showRewardedPearls() {
        showRewarded();
    }

    @Override
    public boolean privacyOptionsRequired() {
        return false;
    }

    @Override
    public void showPrivacyOptions() {
    }
}
