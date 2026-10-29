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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private RelativeLayout rootLayout;
    private LinearLayout mainContainer;
    private FrameLayout clockSection;
    private LinearLayout clockRow;
    private FrameLayout hoursTile;
    private FrameLayout minutesTile;
    private TextView hoursText;
    private TextView minutesText;

    private LinearLayout sideSection;
    private FrameLayout weatherCard;
    private TextView weatherTempText;
    private TextView weatherDescText;
    private TextView weatherHourlyText;

    private FrameLayout btcCard;
    private TextView btcPriceText;
    private TextView btcChangeText;

    private TextView batteryText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
    private final SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

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

    private final Runnable minuteUpdater = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            if (hoursText != null && minutesText != null) {
                hoursText.setText(hourFormat.format(now));
                minutesText.setText(minuteFormat.format(now));
            }

            Calendar cal = Calendar.getInstance();
            int millisUntilNextMinute = (60 - cal.get(Calendar.SECOND)) * 1000 - cal.get(Calendar.MILLISECOND);
            if (millisUntilNextMinute <= 0) millisUntilNextMinute = 60000;

            handler.postDelayed(this, millisUntilNextMinute);
        }
    };

    private final Runnable dataRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            networkExecutor.execute(() -> {
                fetchWeather();
                fetchBtc();
            });
            handler.postDelayed(this, 15 * 60 * 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        rootLayout = new RelativeLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        mainContainer = new LinearLayout(this);
        RelativeLayout.LayoutParams mainParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        );
        rootLayout.addView(mainContainer, mainParams);

        // Horloge
        clockSection = new FrameLayout(this);
        clockRow = new LinearLayout(this);
        clockRow.setOrientation(LinearLayout.HORIZONTAL);
        clockRow.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams rowInClock = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        );
        clockSection.addView(clockRow, rowInClock);

        hoursText = new TextView(this);
        minutesText = new TextView(this);
        hoursTile = createTile(hoursText);
        minutesTile = createTile(minutesText);
        clockRow.addView(hoursTile);
        clockRow.addView(minutesTile);

        // Section Widgets (Météo + BTC)
        sideSection = new LinearLayout(this);
        sideSection.setGravity(Gravity.CENTER);

        // Météo
        weatherCard = createWidgetCard();
        LinearLayout weatherBox = new LinearLayout(this);
        weatherBox.setOrientation(LinearLayout.VERTICAL);
        weatherBox.setGravity(Gravity.CENTER);

        weatherTempText = new TextView(this);
        weatherTempText.setTextColor(Color.WHITE);
        weatherTempText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        weatherTempText.setText("--°C");
        weatherTempText.setGravity(Gravity.CENTER);
        weatherTempText.setSingleLine(true);

        weatherDescText = new TextView(this);
        weatherDescText.setTextColor(Color.parseColor("#9E9E9E"));
        weatherDescText.setText("Ploëzal");
        weatherDescText.setGravity(Gravity.CENTER);

        weatherHourlyText = new TextView(this);
        weatherHourlyText.setTextColor(Color.parseColor("#B0B0B0"));
        weatherHourlyText.setText("--h: --° | --h: --° | --h: --°");
        weatherHourlyText.setGravity(Gravity.CENTER);
        weatherHourlyText.setSingleLine(true);

        weatherBox.addView(weatherTempText);
        weatherBox.addView(weatherDescText);
        weatherBox.addView(weatherHourlyText);
        weatherCard.addView(weatherBox, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ));

        // BTC
        btcCard = createWidgetCard();
        LinearLayout btcBox = new LinearLayout(this);
        btcBox.setOrientation(LinearLayout.VERTICAL);
        btcBox.setGravity(Gravity.CENTER);

        btcPriceText = new TextView(this);
        btcPriceText.setTextColor(Color.WHITE);
        btcPriceText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        btcPriceText.setText("$ --");
        btcPriceText.setGravity(Gravity.CENTER);
        btcPriceText.setSingleLine(true);

        btcChangeText = new TextView(this);
        btcChangeText.setTextColor(Color.parseColor("#9E9E9E"));
        btcChangeText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        btcChangeText.setText("24h: -- %");
        btcChangeText.setGravity(Gravity.CENTER);
        btcChangeText.setSingleLine(true);

        btcBox.addView(btcPriceText);
        btcBox.addView(btcChangeText);
        btcCard.addView(btcBox, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ));

        sideSection.addView(weatherCard);
        sideSection.addView(btcCard);

        mainContainer.addView(clockSection);
        mainContainer.addView(sideSection);

        // Batterie
        batteryText = new TextView(this);
        batteryText.setTextColor(Color.parseColor("#888888"));
        batteryText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        batteryText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        RelativeLayout.LayoutParams battParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        battParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        battParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        int margin = dpToPx(14);
        battParams.setMargins(0, margin, margin, 0);
        rootLayout.addView(batteryText, battParams);

        setContentView(rootLayout);
        hideSystemUI();

        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                applyLayoutRatios();
            }
        });
    }

    private void applyLayoutRatios() {
        int w = rootLayout.getWidth();
        int h = rootLayout.getHeight();
        if (w == 0 || h == 0) return;

        boolean isLandscape = w > h;
        int spacing = dpToPx(12);

        if (isLandscape) {
            mainContainer.setOrientation(LinearLayout.HORIZONTAL);
            sideSection.setOrientation(LinearLayout.VERTICAL);

            // 62 % Horloge, 38 % Widgets pour garantir la place du BTC et météo
            int clockW = (int) (w * 0.62);
            int sideW = w - clockW;

            clockSection.setLayoutParams(new LinearLayout.LayoutParams(clockW, h));
            sideSection.setLayoutParams(new LinearLayout.LayoutParams(sideW, h));

            int tileW = (clockW - spacing * 3) / 2;
            int tileH = Math.min((int) (h * 0.85), (int) (tileW * 1.15));
            updateTileDimensions(tileW, tileH, spacing);

            int cardW = sideW - spacing * 2;
            int cardH = (h - spacing * 3) / 2;
            setCardParams(weatherCard, cardW, cardH, spacing);
            setCardParams(btcCard, cardW, cardH, 0);

            // Tailles de polices ajustées
            weatherTempText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(34, cardH * 0.26f));
            weatherDescText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(13, cardH * 0.11f));
            weatherHourlyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(11, cardH * 0.09f));

            btcPriceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(32, cardH * 0.25f));
            btcChangeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(15, cardH * 0.12f));

        } else {
            mainContainer.setOrientation(LinearLayout.VERTICAL);
            sideSection.setOrientation(LinearLayout.VERTICAL);

            int clockH = (int) (h * 0.48);
            int sideH = h - clockH;

            clockSection.setLayoutParams(new LinearLayout.LayoutParams(w, clockH));
            sideSection.setLayoutParams(new LinearLayout.LayoutParams(w, sideH));

            // En portrait : calcul basé sur la largeur pour éviter le rognage des chiffres
            int tileW = (w - spacing * 3) / 2;
            int tileH = Math.min((int) (clockH * 0.88), (int) (tileW * 1.15));
            updateTileDimensions(tileW, tileH, spacing);

            int cardW = w - spacing * 2;
            int cardH = (sideH - spacing * 3) / 2;
            setCardParams(weatherCard, cardW, cardH, spacing);
            setCardParams(btcCard, cardW, cardH, 0);

            weatherTempText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(38, cardH * 0.28f));
            weatherDescText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(14, cardH * 0.11f));
            weatherHourlyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(12, cardH * 0.09f));

            btcPriceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(42, cardH * 0.30f));
            btcChangeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.min(16, cardH * 0.13f));
        }
    }

    private void updateTileDimensions(int tileW, int tileH, int spacing) {
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(tileW, tileH);
        p1.setMargins(0, 0, spacing / 2, 0);
        hoursTile.setLayoutParams(p1);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(tileW, tileH);
        p2.setMargins(spacing / 2, 0, 0, 0);
        minutesTile.setLayoutParams(p2);

        // Calcul strict anti-rognage : basé sur la plus petite dimension
        float density = getResources().getDisplayMetrics().scaledDensity;
        float optimalSp = (Math.min(tileW * 0.72f, tileH * 0.60f)) / density;
        hoursText.setTextSize(TypedValue.COMPLEX_UNIT_SP, optimalSp);
        minutesText.setTextSize(TypedValue.COMPLEX_UNIT_SP, optimalSp);
    }

    private void setCardParams(FrameLayout card, int width, int height, int marginB) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = marginB;
        card.setLayoutParams(params);
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
        tv.setIncludeFontPadding(false);

        tile.addView(tv, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ));

        View splitLine = new View(this);
        splitLine.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(3)
        );
        lineParams.gravity = Gravity.CENTER;
        tile.addView(splitLine, lineParams);
        return tile;
    }

    private FrameLayout createWidgetCard() {
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#131417"));
        bg.setCornerRadius(dpToPx(14));
        card.setBackground(bg);
        return card;
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

    private void fetchWeather() {
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=48.7107&longitude=-3.2014&current=temperature_2m,weather_code&hourly=temperature_2m,weather_code&timezone=auto");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());

            JSONObject current = json.getJSONObject("current");
            double currentTemp = current.getDouble("temperature_2m");
            int currentCode = current.getInt("weather_code");
            String currentSymbol = getWeatherSymbol(currentCode);

            JSONObject hourly = json.getJSONObject("hourly");
            JSONArray timeArr = hourly.getJSONArray("time");
            JSONArray tempArr = hourly.getJSONArray("temperature_2m");
            JSONArray codeArr = hourly.getJSONArray("weather_code");

            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            StringBuilder hourlySb = new StringBuilder();
            int count = 0;

            for (int i = 0; i < timeArr.length() && count < 3; i++) {
                String timeStr = timeArr.getString(i);
                if (timeStr.length() >= 13) {
                    int h = Integer.parseInt(timeStr.substring(11, 13));
                    if (h > currentHour) {
                        double hTemp = tempArr.getDouble(i);
                        int hCode = codeArr.getInt(i);
                        if (count > 0) hourlySb.append(" | ");
                        hourlySb.append(h).append("h ").append(getWeatherSymbol(hCode)).append(" ").append(Math.round(hTemp)).append("°");
                        count++;
                    }
                }
            }

            final String hourlyResult = hourlySb.toString();

            handler.post(() -> {
                if (weatherTempText != null && weatherDescText != null && weatherHourlyText != null) {
                    weatherTempText.setText(currentSymbol + " " + Math.round(currentTemp) + "°C");
                    weatherDescText.setText("Ploëzal");
                    if (!hourlyResult.isEmpty()) {
                        weatherHourlyText.setText(hourlyResult);
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    private String getWeatherSymbol(int code) {
        if (code == 0) return "☀️";
        if (code >= 1 && code <= 3) return "⛅";
        if (code >= 45 && code <= 48) return "🌫️";
        if (code >= 51 && code <= 67) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌦️";
        if (code >= 95) return "⛈️";
        return "☁️";
    }

    private void fetchBtc() {
        try {
            URL url = new URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd&include_24hr_change=true");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            JSONObject btc = json.getJSONObject("bitcoin");
            long price = Math.round(btc.getDouble("usd"));
            double change24h = btc.getDouble("usd_24h_change");

            handler.post(() -> {
                if (btcPriceText != null && btcChangeText != null) {
                    btcPriceText.setText(String.format(Locale.US, "$ %,d", price).replace(",", " "));
                    String sign = change24h >= 0 ? "+" : "";
                    btcChangeText.setText(String.format(Locale.US, "24h: %s%.2f %%", sign, change24h));
                    btcChangeText.setTextColor(change24h >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#E53935"));
                }
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        handler.post(minuteUpdater);
        handler.post(dataRefreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
        handler.removeCallbacks(minuteUpdater);
        handler.removeCallbacks(dataRefreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdown();
    }
}
