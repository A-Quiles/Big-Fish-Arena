package com.aquiles.bigfisharena;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Bloqueo de versiones: si hay una versión más nueva, el juego queda tapado por una ventana que no se puede
 * cerrar, con un botón que lleva a la ficha del juego en Google Play.
 *
 * Se entera de dos maneras:
 *   1. Google Play (In-App Updates): en cuanto Play ofrece una versión nueva a ese móvil. Automático.
 *   2. https://big-fish-arena.vercel.app/version.json -> "minVersionCode": para forzarlo a mano al instante.
 * Lo último que se supo se guarda en el móvil: sin conexión sigue bloqueado si ya sabía que había versión nueva.
 * Si nunca ha podido comprobarlo (sin conexión, o instalada fuera de Google Play), deja jugar.
 */
final class UpdateGate {
    static final String VERSION_URL = "https://big-fish-arena.vercel.app/version.json";
    private static final String PREFS = "bfa_update";
    private static final String KEY_PLAY = "need_play";       // versión que ofrece Google Play (0 = ninguna)
    private static final String KEY_WEB = "need_web";         // minVersionCode de version.json
    private static final long RECHECK_MS = 10 * 60 * 1000L;   // al volver a la app, como mucho cada 10 min

    interface Listener {
        void onBlocked();

        void onUnblocked();
    }

    private final Activity act;
    private final FrameLayout root;
    private final Listener listener;
    private final SharedPreferences prefs;
    private final long current;
    private View overlay;
    private long lastCheck;

    UpdateGate(Activity act, FrameLayout root, Listener listener) {
        this.act = act;
        this.root = root;
        this.listener = listener;
        this.prefs = act.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.current = versionCode(act);
    }

    boolean blocked() {
        return overlay != null;
    }

    /** Al abrir la app (force) y cada vez que se vuelve a ella. */
    void check(boolean force) {
        apply();
        long now = SystemClock.elapsedRealtime();
        if (!force && lastCheck != 0 && now - lastCheck < RECHECK_MS) return;
        lastCheck = now;
        checkPlay();
        checkWeb();
    }

    // ---------- 1. Google Play ----------
    private void checkPlay() {
        try {
            AppUpdateManager m = AppUpdateManagerFactory.create(act);
            m.getAppUpdateInfo().addOnSuccessListener(info -> {
                int a = info.updateAvailability();
                boolean newer = a == UpdateAvailability.UPDATE_AVAILABLE
                        || a == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS;
                remember(KEY_PLAY, newer ? Math.max(info.availableVersionCode(), current + 1) : 0);
            });
            // si falla (sin conexión, instalada fuera de Google Play...) se queda lo último que se supo
        } catch (Throwable t) {
            CrashReporter.log(act, "UpdateGate Play: " + t);
        }
    }

    // ---------- 2. version.json en la web ----------
    private void checkWeb() {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(VERSION_URL + "?v=" + current).openConnection();
                c.setConnectTimeout(6000);
                c.setReadTimeout(6000);
                c.setUseCaches(false);
                c.setRequestProperty("Cache-Control", "no-cache");
                if (c.getResponseCode() != 200) return;
                JSONObject j = new JSONObject(read(c.getInputStream()));
                long min = Math.max(0, j.optLong("minVersionCode", 0));
                act.runOnUiThread(() -> remember(KEY_WEB, min));
            } catch (Exception ignored) {
                // sin conexión o la web no responde: se queda lo último que se supo
            } finally {
                if (c != null) c.disconnect();
            }
        }, "bfa-version").start();
    }

    private void remember(String key, long code) {
        if (act.isFinishing() || act.isDestroyed()) return;
        prefs.edit().putLong(key, code).apply();
        apply();
    }

    private void apply() {
        long need = Math.max(prefs.getLong(KEY_PLAY, 0), prefs.getLong(KEY_WEB, 0));
        if (need > current) block();
        else unblock();
    }

    // ---------- Ventana de bloqueo ----------
    private void block() {
        if (overlay != null) return;
        CrashReporter.log(act, "Bloqueado por versión antigua: " + current);
        overlay = buildOverlay();
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.requestFocus();
        listener.onBlocked();
    }

    private void unblock() {
        if (overlay == null) return;
        root.removeView(overlay);
        overlay = null;
        listener.onUnblocked();
    }

    void openStore() {
        String pkg = act.getPackageName();
        Intent play = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
        play.setPackage("com.android.vending");
        try {
            act.startActivity(play);
        } catch (ActivityNotFoundException e) {
            try {
                act.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            } catch (ActivityNotFoundException ignored) {
            }
        }
    }

    private View buildOverlay() {
        FrameLayout o = new FrameLayout(act);
        o.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(11, 58, 92), Color.rgb(3, 20, 34)}));
        o.setClickable(true);                     // nada de lo que hay debajo recibe toques
        o.setFocusable(true);
        o.setFocusableInTouchMode(true);

        ScrollView sv = new ScrollView(act);
        sv.setFillViewport(true);
        o.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout wrap = new LinearLayout(act);
        wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        sv.addView(wrap, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(20), dp(24), dp(24));
        card.setBackground(rounded(Color.rgb(15, 45, 63), dp(24), Color.rgb(1, 10, 18)));
        wrap.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView icon = new ImageView(act);
        icon.setImageResource(R.mipmap.ic_launcher_round);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(72), dp(72));
        ilp.bottomMargin = dp(10);
        card.addView(icon, ilp);

        TextView title = text(R.string.update_title, 24, Color.rgb(255, 210, 63), true);
        card.addView(title, wrapLp(0));
        TextView body = text(R.string.update_body, 17, Color.rgb(232, 247, 244), false);
        body.setLineSpacing(0, 1.15f);
        card.addView(body, wrapLp(dp(8)));

        TextView btn = text(R.string.update_button, 20, Color.rgb(11, 34, 51), true);
        btn.setPadding(dp(28), dp(12), dp(28), dp(16));
        GradientDrawable edge = rounded(Color.rgb(11, 34, 51), dp(18), 0);
        GradientDrawable face = rounded(Color.rgb(255, 210, 63), dp(18), Color.rgb(11, 34, 51));
        LayerDrawable ld = new LayerDrawable(new android.graphics.drawable.Drawable[]{edge, face});
        ld.setLayerInset(1, 0, 0, 0, dp(4));     // borde de abajo más grueso, como los botones del juego
        btn.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), ld, null));
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setOnClickListener(v -> openStore());
        card.addView(btn, wrapLp(dp(20)));
        return o;
    }

    private TextView text(int res, float sp, int color, boolean bold) {
        TextView t = new TextView(act);
        t.setText(res);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setMaxWidth(dp(400));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private static LinearLayout.LayoutParams wrapLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top;
        return lp;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (stroke != 0) g.setStroke(dp(3), stroke);
        return g;
    }

    private int dp(float v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }

    private static String read(InputStream in) throws java.io.IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 65536) break;
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @SuppressWarnings("deprecation")
    static long versionCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pi.getLongVersionCode() : pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return Long.MAX_VALUE;                // por si acaso: nunca bloquear por un error nuestro
        }
    }
}
