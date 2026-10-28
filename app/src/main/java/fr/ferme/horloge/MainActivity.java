package fr.ferme.horloge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView hoursText;
    private TextView minutesText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
    private final SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            if (hoursText != null && minutesText != null) {
                hoursText.setText(hourFormat.format(now));
                minutesText.setText(minuteFormat.format(now));
            }
            // Mise à jour chaque seconde
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Écran toujours allumé
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Fond global noir profond
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.parseColor("#0A0A0A"));

        // Conteneur horizontal centré pour les deux tuiles
        LinearLayout tilesRow = new LinearLayout(this);
        tilesRow.setOrientation(LinearLayout.HORIZONTAL);
        tilesRow.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams rowParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Construction des deux tuiles (Heures et Minutes)
        hoursText = new TextView(this);
        minutesText = new TextView(this);

        FrameLayout hoursTile = createFlipTile(hoursText);
        FrameLayout minutesTile = createFlipTile(minutesText);

        // Espacement entre les tuiles
        LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(
            dpToPx(160), dpToPx(190)
        );
        marginParams.setMargins(dpToPx(10), 0, dpToPx(10), 0);

        tilesRow.addView(hoursTile, marginParams);
        tilesRow.addView(minutesTile, marginParams);

        root.addView(tilesRow, rowParams);
        setContentView(root);

        hideSystemUI();
        handler.post(timeUpdater);
    }

    private FrameLayout createFlipTile(TextView textView) {
        FrameLayout tile = new FrameLayout(this);

        // Fond gris foncé avec angles arrondis
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1C1E22"));
        bg.setCornerRadius(dpToPx(14));
        tile.setBackground(bg);

        // Chiffres blancs, police épaisse
        textView.setTextColor(Color.parseColor("#EDEDED"));
        textView.setTextSize(105);
        textView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        textView.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        tile.addView(textView, textParams);

        // Fente horizontale centrale (effet clapet)
        View splitLine = new View(this);
        splitLine.setBackgroundColor(Color.parseColor("#0A0A0A"));
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timeUpdater);
    }
}
