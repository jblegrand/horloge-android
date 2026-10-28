package fr.ferme.horloge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private RelativeLayout rootLayout;
    private LinearLayout clockRow;
    private FrameLayout hoursTile;
    private FrameLayout minutesTile;
    private TextView hoursText;
    private TextView minutesText;
    private TextView batteryText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
    private final SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

    // Récepteur passif pour le niveau de batterie (aucun impact CPU)
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING 
                    || status == BatteryManager.BATTERY_STATUS_FULL;

            if (level >= 0 && scale > 0 && batteryText != null) {
                int pct = (level * 100) / scale;
                batteryText.setText((isCharging ? "⚡ " : "") + pct + " %");
            }
        }
    };

    // Mise à jour alignée exactement sur le changement de minute
    private final Runnable minuteUpdater = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            if (hoursText != null && minutesText != null) {
                hoursText.setText(hourFormat.format(now));
                minutesText.setText(minuteFormat.format(now));
            }

            // Calcul du temps restant jusqu'à la prochaine minute pleine
            Calendar cal = Calendar.getInstance();
            int millisUntilNextMinute = (60 - cal.get(Calendar.SECOND)) * 1000 - cal.get(Calendar.MILLISECOND);
            if (millisUntilNextMinute <= 0) millisUntilNextMinute = 60000;

            handler.postDelayed(this, millisUntilNextMinute);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Maintien actif de l'écran
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Fond 100 % noir (économie maximale d'énergie)
        rootLayout = new RelativeLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        // Ligne des tuiles
        clockRow = new LinearLayout(this);
        clockRow.setOrientation(LinearLayout.HORIZONTAL);
        clockRow.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams rowParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        hoursText = new TextView(this);
        minutesText = new TextView(this);
        hoursTile = createTile(hoursText);
        minutesTile = createTile(minutesText);

        clockRow.addView(hoursTile);
        clockRow.addView(minutesTile);
        rootLayout.addView(clockRow, rowParams);

        // Indicateur de batterie discret en haut à droite
        batteryText = new TextView(this);
        batteryText.setTextColor(Color.parseColor("#777777"));
        batteryText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        batteryText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        RelativeLayout.LayoutParams battParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        battParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        battParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        int m = dpToPx(20);
        battParams.setMargins(0, m, m, 0);
        rootLayout.addView(batteryText, battParams);

        setContentView(rootLayout);
        hideSystemUI();

        // Calcul dynamique de la taille dès que l'écran est mesuré ou pivote
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                resizeLayout();
            }
        });
    }

    private void resizeLayout() {
        int screenWidth = rootLayout.getWidth();
        int screenHeight = rootLayout.getHeight();
        if (screenWidth == 0 || screenHeight == 0) return;

        boolean isLandscape = screenWidth > screenHeight;

        int tileWidth;
        int tileHeight;
        int spacing = dpToPx(16);

        if (isLandscape) {
            // Utilise 90 % de la largeur pour les deux tuiles
            int availableWidth = screenWidth - spacing - dpToPx(32);
            tileWidth = availableWidth / 2;
            tileHeight = (int) (screenHeight * 0.78);
        } else {
            // Mode portrait
            int availableWidth = screenWidth - spacing - dpToPx(16);
            tileWidth = availableWidth / 2;
            tileHeight = (int) (tileWidth * 1.25);
        }

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(tileWidth, tileHeight);
        p1.setMargins(0, 0, spacing / 2, 0);
        hoursTile.setLayoutParams(p1);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(tileWidth, tileHeight);
        p2.setMargins(spacing / 2, 0, 0, 0);
        minutesTile.setLayoutParams(p2);

        // Adaptation de la taille du texte à la hauteur du bloc
        float textSizeSp = (tileHeight * 0.58f) / getResources().getDisplayMetrics().scaledDensity;
        hoursText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        minutesText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }

    private FrameLayout createTile(TextView tv) {
        FrameLayout tile = new FrameLayout(this);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#17181C"));
        bg.setCornerRadius(dpToPx(16));
        tile.setBackground(bg);

        tv.setTextColor(Color.parseColor("#EAEAEA"));
        tv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tv.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        tile.addView(tv, textParams);

        // Ligne centrale de clapet
        View splitLine = new View(this);
        splitLine.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dpToPx(3)
        );
        lineParams.gravity = Gravity.CENTER;
        tile.addView(splitLine, lineParams);

        return tile;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        handler.post(minuteUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
        handler.removeCallbacks(minuteUpdater);
    }
}
