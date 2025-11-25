package com.example.labrite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GameView extends View {
    
    public interface GameListener {
        void onMoveMade(int movesLeft);
        void onLevelCompleted();
        void onGameOver();
    }
    
    private GameListener gameListener;
    private Paint wallPaint, playerPaint, targetPaint, pathPaint, borderPaint;
    private int cellSize;
    private int gridWidth = 10;
    private int gridHeight = 10;
    private boolean isDarkTheme = true;
    private Point playerPos;
    private Point targetPos;
    private boolean[][] walls;
    private int movesLeft;
    private int currentLevel;
    private boolean gameCompleted = false;
    private boolean gameOver = false;
    private boolean isPreparingLevel = false;
    
    // Анимация
    private float playerAnimationX = 0;
    private float playerAnimationY = 0;
    private boolean isAnimating = false;
    private long animationStartTime = 0;
    private Point animationStartPos;
    private Point animationEndPos;
    private Direction animationDirection;
    
    // Алгоритм поиска пути
    private boolean[][] visited;
    private int minMovesToTarget = -1;
    
    // Переменные для определения свайпов
    private float startX, startY;
    private static final float MIN_SWIPE_DISTANCE = 50f;
    
    // Направления движения
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    
    public GameView(Context context) {
        super(context);
        init();
    }
    
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d("PFPUZ", "GameView.onAttachedToWindow");
        post(new Runnable() {
            @Override
            public void run() {
                Log.d("PFPUZ", "Attached, size w=" + getWidth() + " h=" + getHeight());
                invalidate();
            }
        });
    }
    
    private void init() {
        Log.d("PFPUZ", "GameView.init");
        
        // Проверяем тему
        isDarkTheme = SettingsActivity.isDarkTheme(getContext());
        
        wallPaint = new Paint();
        wallPaint.setColor(isDarkTheme ? Color.parseColor("#0F0F10") : Color.parseColor("#BDBDBD"));
        wallPaint.setStyle(Paint.Style.FILL);
        
        playerPaint = new Paint();
        playerPaint.setColor(Color.parseColor("#4A90E2"));
        playerPaint.setStyle(Paint.Style.FILL);
        playerPaint.setAntiAlias(true);
        
        targetPaint = new Paint();
        targetPaint.setColor(Color.parseColor("#E74C3C"));
        targetPaint.setStyle(Paint.Style.FILL);
        targetPaint.setAntiAlias(true);
        
        pathPaint = new Paint();
        pathPaint.setColor(isDarkTheme ? Color.parseColor("#2F3240") : Color.parseColor("#E8F5E8"));
        pathPaint.setStyle(Paint.Style.FILL);
        
        borderPaint = new Paint();
        borderPaint.setColor(isDarkTheme ? Color.parseColor("#2A2E3A") : Color.parseColor("#C8E6C9"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1);
        setBackgroundColor(isDarkTheme ? Color.parseColor("#1E1E1E") : Color.parseColor("#FFFFFF"));
        
        walls = new boolean[gridHeight][gridWidth];
        playerPos = new Point(1, 1);
        targetPos = new Point(gridWidth - 2, gridHeight - 2);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellSize = Math.min(w / gridWidth, h / gridHeight);
        Log.d("PFPUZ", "onSizeChanged w=" + w + " h=" + h + " cellSize=" + cellSize);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("PFPUZ", "onDraw width=" + getWidth() + " height=" + getHeight() + " cell=" + cellSize);
        // Если размер ячейки еще не посчитан, посчитаем на лету
            if (cellSize <= 0) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                cellSize = Math.min(w / gridWidth, h / gridHeight);
            }
            if (cellSize <= 0) {
                    Log.w("PFPUZ", "cellSize not ready yet, skip frame");
                return; // ждем валидного размера
            }
        }

        if (isPreparingLevel) {
            Paint loadingPaint = new Paint();
            loadingPaint.setColor(Color.parseColor("#FFFFFFFF"));
            loadingPaint.setTextSize(Math.max(28, cellSize/3));
            loadingPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Загрузка уровня...", getWidth()/2f, getHeight()/2f, loadingPaint);
            return;
        }
        
        // Рисуем сетку
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int left = x * cellSize;
                int top = y * cellSize;
                int right = left + cellSize;
                int bottom = top + cellSize;
                
                if (walls[y][x]) {
                    canvas.drawRect(left, top, right, bottom, wallPaint);
                } else {
                    canvas.drawRect(left, top, right, bottom, pathPaint);
                }
                
                // Рисуем границы ячеек
                canvas.drawRect(left, top, right, bottom, borderPaint);
            }
        }
        
        // Рисуем цель (пульсирующий эффект)
        int targetLeft = targetPos.x * cellSize;
        int targetTop = targetPos.y * cellSize;
        int targetCenterX = targetLeft + cellSize/2;
        int targetCenterY = targetTop + cellSize/2;
        
        // Создаем пульсирующий эффект для цели
        float pulseScale = 1.0f + 0.2f * (float) Math.sin(System.nanoTime() * 0.0000005);
        canvas.drawCircle(targetCenterX, targetCenterY, (cellSize/3) * pulseScale, targetPaint);
        
        // Рисуем игрока с анимацией
        float playerCenterX = playerPos.x * cellSize + cellSize/2;
        float playerCenterY = playerPos.y * cellSize + cellSize/2;
        
        if (isAnimating) {
            playerCenterX += playerAnimationX;
            playerCenterY += playerAnimationY;
        }
        
        // Добавляем тень для игрока
        Paint shadowPaint = new Paint(playerPaint);
        shadowPaint.setColor(Color.parseColor("#80000000"));
        canvas.drawCircle(playerCenterX + 2, playerCenterY + 2, cellSize/3, shadowPaint);
        
        canvas.drawCircle(playerCenterX, playerCenterY, cellSize/3, playerPaint);
        
        // Рисуем индикатор ходов
        if (movesLeft <= 3 && movesLeft > 0) {
            Paint warningPaint = new Paint();
            warningPaint.setColor(Color.parseColor("#F39C12"));
            warningPaint.setTextSize(cellSize/2);
            warningPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⚠", getWidth()/2, cellSize, warningPaint);
        }

    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameCompleted || gameOver) {
            return true;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                return true;
                
            case MotionEvent.ACTION_UP:
                float endX = event.getX();
                float endY = event.getY();
                
                Direction direction = getSwipeDirection(startX, startY, endX, endY);
                if (direction != null) {
                    movePlayer(direction);
                }
                return true;
        }
        return true;
    }
    
    private Direction getSwipeDirection(float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        
        // Проверяем, достаточно ли длинный свайп
        if (Math.abs(deltaX) < MIN_SWIPE_DISTANCE && Math.abs(deltaY) < MIN_SWIPE_DISTANCE) {
            return null;
        }
        
        // Определяем основное направление свайпа
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            return deltaX > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            return deltaY > 0 ? Direction.DOWN : Direction.UP;
        }
    }
    
    private void movePlayer(Direction direction) {
        if (isPreparingLevel || isAnimating) {
            return;
        }
        Point newPos = calculateNewPosition(playerPos, direction);
        
        // Проверяем, изменилась ли позиция (если нет, то ход не засчитывается)
        if (!newPos.equals(playerPos)) {
            // Запускаем анимацию движения
            animatePlayerMovement(playerPos, newPos);
            
            playerPos = newPos;
            movesLeft--;
            
            if (gameListener != null) {
                gameListener.onMoveMade(movesLeft);
            }
            
            // Проверяем, достиг ли игрок цели
            if (playerPos.equals(targetPos)) {
                gameCompleted = true;
                if (gameListener != null) {
                    gameListener.onLevelCompleted();
                }
            } else if (movesLeft <= 0) {
                gameOver = true;
                if (gameListener != null) {
                    gameListener.onGameOver();
                }
            }
        }
    }
    
    private void animatePlayerMovement(Point from, Point to) {
        isAnimating = true;
        animationStartPos = new Point(from);
        animationEndPos = new Point(to);
        animationStartTime = System.currentTimeMillis();
        
        // Запускаем анимацию с плавным ускорением и торможением
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                long duration = 300; // 300ms для анимации
                
                float startX = from.x * cellSize + cellSize/2;
                float startY = from.y * cellSize + cellSize/2;
                float endX = to.x * cellSize + cellSize/2;
                float endY = to.y * cellSize + cellSize/2;
                
                while (System.currentTimeMillis() - startTime < duration) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = (float) elapsed / duration;
                    
                    // Применяем easing функцию (ease-in-out)
                    float easedProgress = easeInOutCubic(progress);
                    
                    float currentX = startX + (endX - startX) * easedProgress;
                    float currentY = startY + (endY - startY) * easedProgress;
                    
                    playerAnimationX = currentX - (to.x * cellSize + cellSize/2);
                    playerAnimationY = currentY - (to.y * cellSize + cellSize/2);
                    
                    post(new Runnable() {
                        @Override
                        public void run() {
                            invalidate();
                        }
                    });
                    
                    try {
                        Thread.sleep(16); // ~60 FPS
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                isAnimating = false;
                playerAnimationX = 0;
                playerAnimationY = 0;
                
                post(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });
            }
        }).start();
    }
    
    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            float f = 2 * t - 2;
            return 1 + f * f * f / 2;
        }
    }
    
    private Point calculateNewPosition(Point currentPos, Direction direction) {
        Point newPos = new Point(currentPos);
        
        // Двигаемся в направлении до столкновения со стеной
        while (true) {
            Point nextPos = new Point(newPos);
            switch (direction) {
                case UP:
                    nextPos.y--;
                    break;
                case DOWN:
                    nextPos.y++;
                    break;
                case LEFT:
                    nextPos.x--;
                    break;
                case RIGHT:
                    nextPos.x++;
                    break;
            }
            
            if (isValidPosition(nextPos)) {
                newPos = nextPos;
            } else {
                break;
            }
        }
        
        return newPos;
    }
    
    private boolean isValidPosition(Point pos) {
        return pos.x >= 0 && pos.x < gridWidth && 
               pos.y >= 0 && pos.y < gridHeight && 
               !walls[pos.y][pos.x];
    }
    
    public void startLevel(int level, int maxMoves) {
        Log.d("PFPUZ", "GameView.startLevel level=" + level + " maxMovesParam=" + maxMoves);
        currentLevel = level;
        gameCompleted = false;
        gameOver = false;
        isPreparingLevel = true;
        invalidate();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTs = System.currentTimeMillis();
                int attempts = 0;
                int localMinMoves;
                int localMovesLeft = maxMoves;
                boolean ok = false;
                do {
                    // Генерация и расчёт
                    generateLevel(level);
                    playerPos = new Point(1, 1);
                    targetPos = new Point(gridWidth - 2, gridHeight - 2);
                    localMinMoves = calculateMinMoves();
                    if (localMinMoves >= 3) {
                        ok = true;
                        localMovesLeft = Math.max(maxMoves, localMinMoves + 2);
                        break;
                    }
                    attempts++;
                } while ((System.currentTimeMillis() - startTs) < 700 && attempts < 60);
                final int resultMin = ok ? localMinMoves : 4; // подстраховка
                final int resultMoves = ok ? localMovesLeft : Math.max(maxMoves, 6);
                post(new Runnable() {
                    @Override
                    public void run() {
                        minMovesToTarget = resultMin;
                        movesLeft = resultMoves;
                        isPreparingLevel = false;
                        Log.d("PFPUZ", "levelReady minMoves=" + minMovesToTarget + " movesLeft=" + movesLeft);
                        // Сообщаем UI фактическое число ходов до первого хода
                        if (gameListener != null) {
                            gameListener.onMoveMade(movesLeft);
                        }
                        invalidate();
                    }
                });
            }
        }).start();
    }
    
    private void generateLevel(int level) {
        // Очищаем лабиринт
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                walls[y][x] = false;
            }
        }
        
        // Обычные края в одну клетку чтобы не выглядели толстыми
        for (int x = 0; x < gridWidth; x++) {
            walls[0][x] = true; // Верхняя граница
            walls[gridHeight - 1][x] = true; // Нижняя граница
        }
        for (int y = 0; y < gridHeight; y++) {
            walls[y][0] = true; // Левая граница
            walls[y][gridWidth - 1] = true; // Правая граница
        }
        
        // Генерируем специальные паттерны для разных уровней
        generateLevelPattern(level);
    }
    
    private void generateLevelPattern(int level) {
        // Для уровней 1–8 используем рандомную генерацию с анти-двухходовым контролем
        if (level <= 8) {
            generateRandomWithAntiTwoMove(level);
            ensureMinMoves(Math.min(4 + level / 2, 9), 120);
            return;
        }
        // 9+ — смешанный режим (пока оставим случайный с усилением сложности)
        generateRandomLevel(level);
        ensureMinMoves(8, 150);
    }

    private void generateRandomWithAntiTwoMove(int level) {
        int attempts = 0;
        do {
            // базовая случайная генерация
            generateRandomLevel(level);
            // блокируем прямые коридоры по краям (решение за 2 хода Right+Down или Down+Right)
            placeEdgeBlockers();
            minMovesToTarget = calculateMinMoves();
            attempts++;
        } while ((minMovesToTarget < 4 || minMovesToTarget == -1) && attempts < 40);
    }

    private void placeEdgeBlockers() {
        // Не ставим на старт/финиш
        // блок по верхней кромке между стартом и правым краем
        walls[1][Math.max(2, gridWidth / 2)] = true;
        // блок по левой кромке между стартом и нижним краем
        walls[Math.max(2, gridHeight / 2)][1] = true;
        // блок по правой кромке между верхом и целью
        walls[Math.max(2, gridHeight / 2)][gridWidth - 2] = true;
        // блок по нижней кромке между левым и целью
        walls[gridHeight - 2][Math.max(2, gridWidth / 2)] = true;
        // Гарантируем, что рядом со стартом/целью свободно
        walls[1][1] = false;
        walls[gridHeight - 2][gridWidth - 2] = false;
        walls[1][2] = false; walls[2][1] = false;
        walls[gridHeight - 2][gridWidth - 3] = false; walls[gridHeight - 3][gridWidth - 2] = false;
    }
    
    private void generateSimpleLevel() {
        // Минимальный набор препятствий, чтобы путь требовал 4+ ходов, но был очевиден
        // Очищаем внутреннюю область
        for (int y = 1; y < gridHeight - 1; y++) {
            for (int x = 1; x < gridWidth - 1; x++) {
                walls[y][x] = false;
            }
        }
        // Несколько стратегических блокеров у краёв, чтобы исключить маршрут в 2 хода
        walls[1][gridWidth - 3] = true; // блокируем длинный ход по верхнему ряду
        walls[gridHeight - 3][gridWidth - 2] = true; // блокируем прямой спуск у правого края
        // Небольшая перегородка возле старта, не перекрывающая путь
        walls[2][3] = true;
        // Гарантируем старт/финиш свободны
        walls[1][1] = false;
        walls[gridHeight - 2][gridWidth - 2] = false;
        // Дополнительно: если вдруг блокеры создали тупик, освобождаем рядом клетки
        walls[1][2] = false;
        walls[2][1] = false;
    }
    
    private void generateCrossLevel() {
        // Уровень с крестообразным препятствием
        for (int i = 2; i < 8; i++) {
            walls[5][i] = true; // Горизонтальная линия
            walls[i][5] = true; // Вертикальная линия
        }
    }
    
    private void generateSpiralLevel() {
        // Спиральный лабиринт
        int centerX = gridWidth / 2;
        int centerY = gridHeight / 2;
        
        for (int i = 0; i < 3; i++) {
            walls[centerY - 1][centerX - 1 + i] = true;
            walls[centerY + 1][centerX - 1 + i] = true;
            walls[centerY - 1 + i][centerX - 1] = true;
            walls[centerY - 1 + i][centerX + 1] = true;
        }
    }
    
    private void generateMazeLevel() {
        // Создаем лабиринт с помощью простого алгоритма
        for (int y = 2; y < gridHeight - 2; y += 2) {
            for (int x = 2; x < gridWidth - 2; x += 2) {
                walls[y][x] = true;
                
                // Добавляем случайные соединения
                if (Math.random() < 0.5 && x + 1 < gridWidth - 1) {
                    walls[y][x + 1] = true;
                }
                if (Math.random() < 0.5 && y + 1 < gridHeight - 1) {
                    walls[y + 1][x] = true;
                }
            }
        }
    }
    
    private void generateRandomLevel(int level) {
        int obstacleCount = Math.min(level * 3, 20);
        
        for (int i = 0; i < obstacleCount; i++) {
            int x = (int) (Math.random() * (gridWidth - 2)) + 1;
            int y = (int) (Math.random() * (gridHeight - 2)) + 1;
            
            // Не ставим препятствие на стартовую или конечную позицию
            if ((x == 1 && y == 1) || (x == gridWidth - 2 && y == gridHeight - 2)) {
                continue;
            }
            
            walls[y][x] = true;
        }
    }

    // Повышает минимальное число ходов до порога, добавляя перегородки, сохраняя проходимость
    private void ensureMinMoves(int minRequiredMoves, int maxAttempts) {
        int attempts = 0;
        while (attempts < maxAttempts) {
            minMovesToTarget = calculateMinMoves();
            if (minMovesToTarget >= minRequiredMoves && minMovesToTarget != -1) {
                return;
            }
            // Добавляем небольшую перегородку в случайном месте, не перекрывая старт/финиш
            int x = (int) (Math.random() * (gridWidth - 4)) + 2;
            int y = (int) (Math.random() * (gridHeight - 4)) + 2;
            if (!walls[y][x] && !(x == 1 && y == 1) && !(x == gridWidth - 2 && y == gridHeight - 2)) {
                walls[y][x] = true;
            }
            attempts++;
        }
        // финальная проверка
        minMovesToTarget = calculateMinMoves();
    }
    
    private int calculateMinMoves() {
        // Инициализируем массив посещенных позиций
        visited = new boolean[gridHeight][gridWidth];
        
        // Используем BFS для поиска кратчайшего пути
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(new PathNode(playerPos, 0));
        visited[playerPos.y][playerPos.x] = true;
        
        while (!queue.isEmpty()) {
            PathNode current = queue.poll();
            
            // Если достигли цели
            if (current.position.equals(targetPos)) {
                return current.moves;
            }
            
            // Проверяем все 4 направления
            for (Direction direction : Direction.values()) {
                Point newPos = calculateNewPosition(current.position, direction);
                
                // Если позиция валидна и не посещена
                if (isValidPosition(newPos) && !visited[newPos.y][newPos.x]) {
                    visited[newPos.y][newPos.x] = true;
                    queue.add(new PathNode(newPos, current.moves + 1));
                }
            }
        }
        
        // Если путь не найден
        return -1;
    }
    
    private static class PathNode {
        Point position;
        int moves;
        
        PathNode(Point position, int moves) {
            this.position = position;
            this.moves = moves;
        }
    }
    
    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }
    
    public void updateTheme() {
        // Обновляем тему и перерисовываем
        isDarkTheme = SettingsActivity.isDarkTheme(getContext());
        
        // Обновляем цвета
        wallPaint.setColor(isDarkTheme ? Color.parseColor("#0F0F10") : Color.parseColor("#BDBDBD"));
        pathPaint.setColor(isDarkTheme ? Color.parseColor("#2F3240") : Color.parseColor("#E8F5E8"));
        borderPaint.setColor(isDarkTheme ? Color.parseColor("#2A2E3A") : Color.parseColor("#C8E6C9"));
        setBackgroundColor(isDarkTheme ? Color.parseColor("#1E1E1E") : Color.parseColor("#FFFFFF"));
        
        // Перерисовываем
        invalidate();
    }
}
