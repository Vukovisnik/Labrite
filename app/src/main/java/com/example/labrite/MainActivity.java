package com.example.labrite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private GameView gameView;
    private TextView levelText;
    private TextView movesText;
    private TextView highScoreText;
    private TextView titleTextView;
    private TextView subtitleTextView;
    private TextView instructionLine1;
    private TextView instructionLine2;
    private View rootLayout;
    private int currentLevel = 1;
    private int maxMoves = 10;
    private int highScore = 0;
    private int maxLevelReached = 0;
    
    // Элементы меню
    private View menuView;
    private View gameViewContainer;
    private Button startGameButton;
    private Button settingsButton;
    private TextView menuHighScoreText;
    
    // SharedPreferences для сохранения рекорда
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LabriteGamePrefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_MAX_LEVEL = "max_level";
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    
    // Звуковые эффекты и вибрация
    private Vibrator vibrator;
    private SoundPool soundPool;
    private int moveSoundId, winSoundId, loseSoundId;
    
    // Обработчик для автоматического перехода
    private Handler autoNextHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Применяем тему перед установкой layout
        applyTheme();
        
        // Всегда используем основной layout, цвета будем менять динамически
        setContentView(R.layout.activity_main);
        
        Log.d("PFPUZ", "MainActivity.onCreate");
        
        // Инициализируем SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadHighScore();
        
        initializeViews();
        initializeSoundAndVibration();
        setupGame();
        showMenu();
        
        // Применяем текущую тему к UI
        boolean isDarkTheme = SettingsActivity.isDarkTheme(this);
        updateUIColors(isDarkTheme);

        // Слушаем изменения настроек для синхронного обновления темы
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (SettingsActivity.KEY_THEME.equals(key)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTheme();
                        }
                    });
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
    
    private void initializeViews() {
        // Игровые элементы
        rootLayout = findViewById(R.id.rootLayout);
        gameView = findViewById(R.id.gameView);
        levelText = findViewById(R.id.levelText);
        movesText = findViewById(R.id.movesText);
        highScoreText = findViewById(R.id.highScoreText);
        titleTextView = findViewById(R.id.titleText);
        subtitleTextView = findViewById(R.id.subtitleText);
        instructionLine1 = findViewById(R.id.instructionLine1);
        instructionLine2 = findViewById(R.id.instructionLine2);
        
        // Элементы меню
        startGameButton = findViewById(R.id.startGameButton);
        settingsButton = findViewById(R.id.settingsButton);
        menuHighScoreText = findViewById(R.id.menuHighScoreText);
        
        // Настройка кнопок меню
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });
        
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 1);
            }
        });
    }
    
    private void initializeSoundAndVibration() {
        // Инициализируем вибрацию
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Инициализируем звуковые эффекты (заглушки, так как нет звуковых файлов)
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build();
        
        // Здесь можно загрузить звуковые файлы, если они есть
        // moveSoundId = soundPool.load(this, R.raw.move_sound, 1);
        // winSoundId = soundPool.load(this, R.raw.win_sound, 1);
        // loseSoundId = soundPool.load(this, R.raw.lose_sound, 1);
    }
    
    private void setupGame() {
        gameView.setGameListener(new GameView.GameListener() {
            @Override
            public void onMoveMade(int movesLeft) {
                updateMovesText(movesLeft);
                playMoveEffect();
            }
            
            @Override
            public void onLevelCompleted() {
                playWinEffect();
                showLevelCompletedToast();
                // Автоматический переход на следующий уровень через 1 секунду
                autoNextHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nextLevel();
                    }
                }, 1000);
            }
            
            @Override
            public void onGameOver() {
                playLoseEffect();
                showGameOver();
            }
        });
    }
    
    private void showMenu() {
        // Показываем меню, скрываем игру
        findViewById(R.id.topPanel).setVisibility(View.GONE);
        findViewById(R.id.gameView).setVisibility(View.GONE);
        findViewById(R.id.bottomPanel).setVisibility(View.GONE);
        
        // Обновляем рекорд в меню
        menuHighScoreText.setText("Рекорд: " + highScore);
    }
    
    private void startGame() {
        // Скрываем меню, показываем игру
        findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
        findViewById(R.id.gameView).setVisibility(View.VISIBLE);
        findViewById(R.id.bottomPanel).setVisibility(View.VISIBLE);
        
        // Сбрасываем игру к первому уровню
        currentLevel = 1;
        maxLevelReached = 0;
        startLevel(currentLevel);
    }
    
    private void startLevel(int level) {
        Log.d("PFPUZ", "startLevel level=" + level);
        maxMoves = getMaxMovesForLevel(level);
        gameView.startLevel(level, maxMoves);
        updateUI();
    }
    
    private int getMaxMovesForLevel(int level) {
        // Увеличиваем сложность с каждым уровнем
        return Math.max(5, 15 - level);
    }
    
    private void updateUI() {
        levelText.setText("Уровень: " + currentLevel);
        movesText.setText("Ходы: " + maxMoves);
        highScoreText.setText("Рекорд: " + highScore);
    }
    
    private void updateMovesText(int movesLeft) {
        movesText.setText("Ходы: " + movesLeft);
    }
    
    private void restartLevel() {
        // Отменяем автоматический переход если он был запланирован
        autoNextHandler.removeCallbacksAndMessages(null);
        showMenu();
    }
    
    private void nextLevel() {
        // Обновляем максимальный достигнутый уровень
        if (currentLevel > maxLevelReached) {
            maxLevelReached = currentLevel;
            updateHighScore();
        }
        
        currentLevel++;
        startLevel(currentLevel);
    }
    
    private void showGameOver() {
        // Обновляем рекорд перед показом диалога
        if (currentLevel > maxLevelReached) {
            maxLevelReached = currentLevel;
            updateHighScore();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Игра окончена!")
               .setMessage("У вас закончились ходы! Вы достигли уровня " + currentLevel + 
                          "\nРекорд: " + highScore + " уровней")
               .setPositiveButton("В главное меню", (dialog, which) -> showMenu())
               .setNegativeButton("Выход", (dialog, which) -> finish())
               .setCancelable(false)
               .show();
    }
    
    private void showLevelCompletedToast() {
        // Создаем кастомный Toast с темной темой
        Toast toast = new Toast(this);
        View toastView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        TextView toastText = toastView.findViewById(android.R.id.text1);
        toastText.setText("🎉 Уровень " + currentLevel + " пройден!");
        toastText.setTextColor(getResources().getColor(android.R.color.white));
        toastText.setBackgroundColor(getResources().getColor(android.R.color.black));
        toastText.setPadding(32, 16, 32, 16);
        
        toast.setView(toastView);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 220);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }
    
    private void playMoveEffect() {
        // Короткая вибрация при движении (если включена)
        if (vibrator != null && vibrator.hasVibrator() && SettingsActivity.isVibrationEnabled(this)) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        
        // Воспроизводим звук движения (если есть)
        if (soundPool != null && moveSoundId != 0) {
            soundPool.play(moveSoundId, 0.5f, 0.5f, 1, 0, 1.0f);
        }
    }
    
    private void playWinEffect() {
        // Длинная вибрация при победе (если включена)
        if (vibrator != null && vibrator.hasVibrator() && SettingsActivity.isVibrationEnabled(this)) {
            long[] pattern = {0, 100, 50, 100, 50, 100};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
        
        // Воспроизводим звук победы (если есть)
        if (soundPool != null && winSoundId != 0) {
            soundPool.play(winSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
    
    private void playLoseEffect() {
        // Вибрация при поражении (если включена)
        if (vibrator != null && vibrator.hasVibrator() && SettingsActivity.isVibrationEnabled(this)) {
            long[] pattern = {0, 200, 100, 200};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
        
        // Воспроизводим звук поражения (если есть)
        if (soundPool != null && loseSoundId != 0) {
            soundPool.play(loseSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
    
    private void loadHighScore() {
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0);
        maxLevelReached = prefs.getInt(KEY_MAX_LEVEL, 0);
        Log.d("PFPUZ", "loadHighScore: " + highScore + " maxLevel: " + maxLevelReached);
    }
    
    private void updateHighScore() {
        if (maxLevelReached > highScore) {
            highScore = maxLevelReached;
            prefs.edit()
                 .putInt(KEY_HIGH_SCORE, highScore)
                 .putInt(KEY_MAX_LEVEL, maxLevelReached)
                 .apply();
            Log.d("PFPUZ", "updateHighScore: " + highScore);
        }
    }
    
    private void restartFromLevel1() {
        currentLevel = 1;
        maxLevelReached = 0;
        startLevel(currentLevel);
    }
    
    private void applyTheme() {
        boolean isDarkTheme = SettingsActivity.isDarkTheme(this);
        if (isDarkTheme) {
            setTheme(R.style.Theme_Labrite_Dark);
        } else {
            setTheme(R.style.Theme_Labrite_Light);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("theme_changed", false)) {
                // Тема изменилась, обновляем UI
                updateTheme();
            }
        }
    }
    
    private void updateTheme() {
        // Обновляем активную тему
        applyTheme();

        // Обновляем цвета в GameView
        if (gameView != null) {
            gameView.updateTheme();
        }
        
        // Обновляем цвета UI элементов
        boolean isDarkTheme = SettingsActivity.isDarkTheme(this);
        updateUIColors(isDarkTheme);
    }
    
    private void updateUIColors(boolean isDarkTheme) {
        int backgroundColor = isDarkTheme ? Color.parseColor("#1E1E1E") : Color.parseColor("#F5F5F5");

        // Обновляем фон главного экрана
        findViewById(android.R.id.content).setBackgroundColor(backgroundColor);
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(backgroundColor);
        }

        // Обновляем цвета панелей
        if (isDarkTheme) {
            // Темная тема
            findViewById(R.id.topPanel).setBackgroundColor(Color.parseColor("#2D2D2D"));
            findViewById(R.id.bottomPanel).setBackgroundColor(Color.parseColor("#2D2D2D"));
            levelText.setTextColor(Color.parseColor("#FFFFFF"));
            movesText.setTextColor(Color.parseColor("#FFFFFF"));
            highScoreText.setTextColor(Color.parseColor("#4CAF50"));
            menuHighScoreText.setTextColor(Color.parseColor("#4CAF50"));
            if (titleTextView != null) {
                titleTextView.setTextColor(Color.parseColor("#4CAF50"));
            }
            if (subtitleTextView != null) {
                subtitleTextView.setTextColor(Color.parseColor("#81C784"));
            }
            if (instructionLine1 != null) {
                instructionLine1.setTextColor(Color.parseColor("#81C784"));
            }
            if (instructionLine2 != null) {
                instructionLine2.setTextColor(Color.parseColor("#81C784"));
            }
            if (startGameButton != null) {
                startGameButton.setBackgroundResource(R.drawable.menu_button_bg);
                startGameButton.setTextColor(Color.parseColor("#FFFFFF"));
            }
            if (settingsButton != null) {
                settingsButton.setBackgroundResource(R.drawable.menu_button_bg);
                settingsButton.setTextColor(Color.parseColor("#FFFFFF"));
            }
        } else {
            // Светлая тема
            findViewById(R.id.topPanel).setBackgroundColor(Color.parseColor("#E8F5E8"));
            findViewById(R.id.bottomPanel).setBackgroundColor(Color.parseColor("#E8F5E8"));
            levelText.setTextColor(Color.parseColor("#212121"));
            movesText.setTextColor(Color.parseColor("#212121"));
            highScoreText.setTextColor(Color.parseColor("#4CAF50"));
            menuHighScoreText.setTextColor(Color.parseColor("#2E7D32"));
            if (titleTextView != null) {
                titleTextView.setTextColor(Color.parseColor("#2E7D32"));
            }
            if (subtitleTextView != null) {
                subtitleTextView.setTextColor(Color.parseColor("#388E3C"));
            }
            if (instructionLine1 != null) {
                instructionLine1.setTextColor(Color.parseColor("#388E3C"));
            }
            if (instructionLine2 != null) {
                instructionLine2.setTextColor(Color.parseColor("#388E3C"));
            }
            if (startGameButton != null) {
                startGameButton.setBackgroundResource(R.drawable.menu_button_bg_light);
                startGameButton.setTextColor(Color.parseColor("#212121"));
            }
            if (settingsButton != null) {
                settingsButton.setBackgroundResource(R.drawable.menu_button_bg_light);
                settingsButton.setTextColor(Color.parseColor("#212121"));
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (prefs != null && preferenceChangeListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        if (soundPool != null) {
            soundPool.release();
        }
    }
}