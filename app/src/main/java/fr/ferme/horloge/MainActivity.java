package fr.ferme.horloge;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import java.util.Random;

public class MainActivity extends Activity {

    private LinearLayout clockContainer;
    private final Handler burnInHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Maintient l'écran allumé
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Conteneur principal plein écran noir
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // Conteneur centré pour l'horloge
        clockContainer = new LinearLayout(this);
        clockContainer.setOrientation(LinearLayout.VERTICAL);
        clockContainer.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Affichage de l'heure
        TextClock timeClock = new TextClock(this);
        timeClock.setFormat12Hour("HH:mm");
        timeClock.setFormat24Hour("HH:mm");
        timeClock.setTextColor(Color.WHITE);
        timeClock.setTextSize(90);

        // Affichage de la date
        TextClock dateClock = new TextClock(this);
        dateClock.setFormat12Hour("EEEE d MMMM yyyy");
        dateClock.setFormat24Hour("EEEE d MMMM yyyy");
        dateClock.setTextColor(Color.GRAY);
        dateClock.setTextSize(22);

        clockContainer.addView(timeClock);
        clockContainer.addView(dateClock);
        root.addView(clockContainer, containerParams);

        setContentView(root);
        hideSystemUI();
        startBurnInProtection();
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

    // Déplace le bloc de quelques pixels toutes les minutes pour ménager l'écran
    private void startBurnInProtection() {
        burnInHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (clockContainer != null) {
                    float offsetX = (random.nextFloat() - 0.5f) * 40;
                    float offsetY = (random.nextFloat() - 0.5f) * 40;
                    clockContainer.setTranslationX(offsetX);
                    clockContainer.setTranslationY(offsetY);
                }
                burnInHandler.postDelayed(this, 60000);
            }
        }, 60000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
}
