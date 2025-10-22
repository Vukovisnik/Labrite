package com.example.labrite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private GameView gameView;
    private TextView levelText;
    private TextView movesText;
    private TextView highScoreText;
    private ImageButton restartButton;
    private int currentLevel = 1;
    private int maxMoves = 10;
    private int highScore = 0;
    private int maxLevelReached = 0;
    
    // SharedPreferences для сохранения рекорда
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LabriteGamePrefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_MAX_LEVEL = "max_level";
    
    // Звуковые эффекты и вибрация
    private Vibrator vibrator;
    private SoundPool soundPool;
    private int moveSoundId, winSoundId, loseSoundId;
    
    // Обработчик для автоматического перехода
    private Handler autoNextHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("PFPUZ", "MainActivity.onCreate");
        
        // Инициализируем SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadHighScore();
        
        initializeViews();
        initializeSoundAndVibration();
        setupGame();

        // Визуальная проверка старта UI
        Toast.makeText(this, "Игра запускается", Toast.LENGTH_SHORT).show();
    }
    
    private void initializeViews() {
        gameView = findViewById(R.id.gameView);
        levelText = findViewById(R.id.levelText);
        movesText = findViewById(R.id.movesText);
        highScoreText = findViewById(R.id.highScoreText);
        restartButton = findViewById(R.id.restartButton);
        
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartLevel();
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
        startLevel(currentLevel);
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
               .setPositiveButton("Начать заново", (dialog, which) -> restartFromLevel1())
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
        // Короткая вибрация при движении
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        
        // Воспроизводим звук движения (если есть)
        if (soundPool != null && moveSoundId != 0) {
            soundPool.play(moveSoundId, 0.5f, 0.5f, 1, 0, 1.0f);
        }
    }
    
    private void playWinEffect() {
        // Длинная вибрация при победе
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 100, 50, 100, 50, 100};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
        
        // Воспроизводим звук победы (если есть)
        if (soundPool != null && winSoundId != 0) {
            soundPool.play(winSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
    
    private void playLoseEffect() {
        // Вибрация при поражении
        if (vibrator != null && vibrator.hasVibrator()) {
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
        }
    }
}