package com.example.labrite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    
    private Switch themeSwitch;
    private Switch vibrationSwitch;
    private Button backButton;
    private TextView themeText;
    private TextView vibrationText;
    private TextView titleText;
    private LinearLayout themeLayout;
    private LinearLayout vibrationLayout;
    private LinearLayout descriptionLayout;
    
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LabriteGamePrefs";
    public static final String KEY_THEME = "theme";
    private static final String KEY_VIBRATION = "vibration";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Применяем тему перед установкой layout
        applyTheme();
        
        // Всегда используем основной layout, цвета будем менять динамически
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        loadSettings();
        setupListeners();
        
        // Применяем текущую тему к UI
        boolean isDarkTheme = prefs.getBoolean(KEY_THEME, true);
        updateUIColors(isDarkTheme);
    }
    
    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        backButton = findViewById(R.id.backButton);
        themeText = findViewById(R.id.themeText);
        vibrationText = findViewById(R.id.vibrationText);
        titleText = findViewById(R.id.titleText);
        themeLayout = findViewById(R.id.themeLayout);
        vibrationLayout = findViewById(R.id.vibrationLayout);
        descriptionLayout = findViewById(R.id.descriptionLayout);
    }
    
    private void loadSettings() {
        boolean isDarkTheme = prefs.getBoolean(KEY_THEME, true);
        boolean vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        
        themeSwitch.setChecked(isDarkTheme);
        vibrationSwitch.setChecked(vibrationEnabled);
        
        updateThemeText(isDarkTheme);
        updateVibrationText(vibrationEnabled);
    }
    
    private void setupListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_THEME, isChecked).apply();
                updateThemeText(isChecked);
                // Уведомляем MainActivity об изменении темы
                notifyThemeChanged();
            }
        });
        
        vibrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
                updateVibrationText(isChecked);
            }
        });
    }
    
    private void updateThemeText(boolean isDark) {
        themeText.setText("Тема: " + (isDark ? "Темная" : "Светлая"));
    }
    
    private void updateVibrationText(boolean isEnabled) {
        vibrationText.setText("Вибрация: " + (isEnabled ? "Включена" : "Выключена"));
    }
    
    private void applyTheme() {
        boolean isDarkTheme = prefs.getBoolean(KEY_THEME, true);
        if (isDarkTheme) {
            setTheme(R.style.Theme_Labrite_Dark);
        } else {
            setTheme(R.style.Theme_Labrite_Light);
        }
    }
    
    private void notifyThemeChanged() {
        // Обновляем цвета в настройках
        boolean isDarkTheme = prefs.getBoolean(KEY_THEME, true);
        updateUIColors(isDarkTheme);
        
        // Отправляем результат обратно в MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("theme_changed", true);
        setResult(RESULT_OK, resultIntent);
    }
    
    private void updateUIColors(boolean isDarkTheme) {
        // Обновляем фон главного контейнера
        int backgroundColor = isDarkTheme ? Color.parseColor("#1E1E1E") : Color.parseColor("#F5F5F5");
        findViewById(android.R.id.content).setBackgroundColor(backgroundColor);

        // Обновляем фон секций
        int cardBackground = isDarkTheme ? Color.parseColor("#2D2D2D") : Color.parseColor("#FFFFFF");
        if (themeLayout != null) {
            themeLayout.setBackgroundColor(cardBackground);
        }
        if (vibrationLayout != null) {
            vibrationLayout.setBackgroundColor(cardBackground);
        }
        if (descriptionLayout != null) {
            descriptionLayout.setBackgroundColor(cardBackground);
        }

        // Обновляем фон кнопки
        int buttonBackground = isDarkTheme ? R.drawable.menu_button_bg : R.drawable.menu_button_bg_light;
        backButton.setBackgroundResource(buttonBackground);
        backButton.setTextColor(isDarkTheme ? Color.parseColor("#FFFFFF") : Color.parseColor("#212121"));

        // Обновляем цвета текстов
        if (isDarkTheme) {
            // Темная тема
            titleText.setTextColor(Color.parseColor("#4CAF50"));
            themeText.setTextColor(Color.parseColor("#FFFFFF"));
            vibrationText.setTextColor(Color.parseColor("#FFFFFF"));
            
            // Обновляем цвета описаний
            if (descriptionLayout != null) {
                for (int i = 0; i < descriptionLayout.getChildCount(); i++) {
                    View child = descriptionLayout.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(Color.parseColor("#81C784"));
                    }
                }
            }
        } else {
            // Светлая тема
            titleText.setTextColor(Color.parseColor("#4CAF50"));
            themeText.setTextColor(Color.parseColor("#212121"));
            vibrationText.setTextColor(Color.parseColor("#212121"));
            
            // Обновляем цвета описаний
            if (descriptionLayout != null) {
                for (int i = 0; i < descriptionLayout.getChildCount(); i++) {
                    View child = descriptionLayout.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(Color.parseColor("#4CAF50"));
                    }
                }
            }
        }
    }
    
    public static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_THEME, true);
    }
    
    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VIBRATION, true);
    }
}
