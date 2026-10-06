package com.aquiles.bigfisharena;

/**
 * Anuncios de la app. Hay dos versiones de AdsImpl y gradle.properties (ADS_ENABLED) elige cuál se compila:
 *   - src/ads/java   -> AdMob + consentimiento RGPD (UMP), como antes
 *   - src/noads/java -> sin anuncios: ni siquiera se incluye el SDK de AdMob en la app
 */
interface Ads {
    /** Lo que los anuncios necesitan de la pantalla del juego. */
    interface Host {
        void js(String code);

        void hideSystemBars();
    }

    boolean enabled();

    /** Pide el consentimiento (si hace falta) y empieza a cargar anuncios. */
    void start();

    void showInterstitial();

    void showRewarded();

    void showRewardedPearls();

    boolean privacyOptionsRequired();

    void showPrivacyOptions();
}
