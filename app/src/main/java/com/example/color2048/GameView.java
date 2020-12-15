package com.example.color2048;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.color2048.gameModel.AnimationCell;
import com.example.color2048.gameModel.MainGame;
import com.example.color2048.gameModel.Tile;
import com.example.color2048.utility.Utils;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class GameView extends View {

    public static final int BASE_ANIMATION_TIME = 100000000;
    private static final float MERGING_ACCELERATION = (float) -0.5;
    private static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;
    public final int numCellTypes = 12;
    private final BitmapDrawable[] bitmapCell = new BitmapDrawable[numCellTypes];
    public MainGame game;
    private final Paint paint = new Paint();
    public boolean continueButtonEnabled = false;
    public int startingX;
    public int startingY;
    public int endingX;
    public int endingY;
    public boolean refreshLastTime = true;

    //для расчета времени между кадрами анимаций
    private long lastFPSTime = System.nanoTime();

    //переменные для разметки
    private int cellSize = 0;
    private int gridWidth = 0;

    private Drawable backgroundRectangle;
    private Drawable lightUpRectangle;
    private Drawable fadeRectangle;
    private Bitmap background = null;
    private BitmapDrawable loseGameOverlay;
    private BitmapDrawable winGameOverlay;
    private AppCompatTextView textViewScore;
    private AppCompatTextView textViewHighScore;

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GameView(Context context) {
        super(context);
    }

    public void init(Context context, MainGame game, AppCompatTextView textViewScore, AppCompatTextView textViewHighScore) {
        this.game = game;
        this.textViewScore = textViewScore;
        this.textViewHighScore = textViewHighScore;

        Resources resources = context.getResources();
        backgroundRectangle = resources.getDrawable(R.drawable.background_rectangle);
        lightUpRectangle = resources.getDrawable(R.drawable.win_rectangle);
        fadeRectangle = resources.getDrawable(R.drawable.lose_rectangle);
        this.setBackgroundColor(resources.getColor(R.color.background));
        paint.setAntiAlias(true);
        setOnTouchListener(new SwipeListener(this));
    }

    private static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(background, 0, 0, paint);

        if (game == null)
            return;

        viewScoreText();
        drawCells(canvas);
        if (!game.isActive())
            drawEndGameState(canvas);

        // обновить экран если есть активные анимации
        if (game.aGrid.isAnimationActive()) {
            invalidate(startingX, startingY, endingX, endingY);
            tick();
            //обновить в последний раз, когда игра завершилась
        } else if (!game.isActive() && refreshLastTime) {
            invalidate();
            refreshLastTime = false;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH) {
        super.onSizeChanged(width, height, oldW, oldH);
        getLayout(width, height);
        createBitmapCells();
        createBackgroundBitmap(width, height);
        createOverlays();
    }

    private void drawDrawable(Canvas canvas, Drawable draw, int startingX, int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }

    private void viewScoreText() {
        textViewScore.setText(String.valueOf(game.score));
        textViewHighScore.setText(String.valueOf(game.highScore));
    }

    private void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY);
    }

    private void drawBackgroundGrid(Canvas canvas) {
        Resources resources = getResources();
        Drawable backgroundCell = resources.getDrawable(R.drawable.cell_rectangle);
        for (int xx = 0; xx < game.getFieldSize(); xx++) {
            for (int yy = 0; yy < game.getFieldSize(); yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                drawDrawable(canvas, backgroundCell, sX, sY, eX, eY);
            }
        }
    }

    private void drawCells(Canvas canvas) {

        float textSize = 0;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int xx = 0; xx < game.getFieldSize(); xx++) {
            for (int yy = 0; yy < game.getFieldSize(); yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                Tile currentTile = game.grid.getCellContent(xx, yy);
                if (currentTile != null) {
                    int value = currentTile.getValue();
                    int index = log2(value);

                    //проверяем на наличие анимаций и запускаем активные
                    ArrayList<AnimationCell> aArray = game.aGrid.getAnimationCell(xx, yy);
                    boolean animated = false;
                    for (int i = aArray.size() - 1; i >= 0; i--) {
                        AnimationCell aCell = aArray.get(i);
                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION)
                            animated = true;
                        if (!aCell.isActive())
                            continue;

                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) { // появление клетки
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
                            bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MERGE_ANIMATION) { // объединение клеток
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (1 + INITIAL_VELOCITY * percentDone
                                    + MERGING_ACCELERATION * percentDone * percentDone / 2);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
                            bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MOVE_ANIMATION) {  // движение клетки
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.extras[0];
                            int previousY = aCell.extras[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1) * 1.0);
                            int dY = (int) ((currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1) * 1.0);
                            bitmapCell[tempIndex].setBounds(sX + dX, sY + dY, eX + dX, eY + dY);
                            bitmapCell[tempIndex].draw(canvas);
                        }
                        animated = true;
                    }

                    //отрисовка клетки, если нет анимаций
                    if (!animated) {
                        bitmapCell[index].setBounds(sX, sY, eX, eY);
                        bitmapCell[index].draw(canvas);
                    }
                }
            }
        }
    }

    private void drawEndGameState(Canvas canvas) {
        double alphaChange = 1;
        continueButtonEnabled = false;

        for (AnimationCell animation : game.aGrid.globalAnimation)
            if (animation.getAnimationType() == MainGame.FADE_GLOBAL_ANIMATION)
                alphaChange = animation.getPercentageDone();

        BitmapDrawable displayOverlay = null;

        if (game.gameWon())
            displayOverlay = winGameOverlay;
        else if (game.gameLost())
            displayOverlay = loseGameOverlay;

        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY);
            displayOverlay.setAlpha((int) (255 * alphaChange));
            displayOverlay.draw(canvas);
        }
        if (!game.isEndDialogShown() && alphaChange == 1.0f) {
            game.setEndDialogShown(true);
            int title = game.gameWon() ? R.string.win_dialog_title : R.string.lose_dialog_title;
            int msg = game.gameWon() ? R.string.win_dialog_message : R.string.lose_dialog_message;
            new AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setPositiveButton(R.string.play_again, (dialog, which) -> game.newGame())
                    .setNegativeButton(R.string.main_menu, (dialogInterface, i) -> {
                        Activity a = Utils.getActivity(getContext());
                        a.finish();
                    })
                    .setTitle(title)
                    .setMessage(msg)
                    .show();
        }
    }

    private void createBackgroundBitmap(int width, int height) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        if (game == null)
            return;
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
    }

    private void createBitmapCells() {
        Resources resources = getResources();
        int[] cellRectangleIds = getCellRectangleIds();
        for (int xx = 1; xx < bitmapCell.length; xx++) {
            Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawDrawable(canvas, resources.getDrawable(cellRectangleIds[xx]), 0, 0, cellSize, cellSize);
            bitmapCell[xx] = new BitmapDrawable(resources, bitmap);
        }
    }

    private int[] getCellRectangleIds() {
        int[] cellRectangleIds = new int[numCellTypes];
        cellRectangleIds[0] = R.drawable.cell_rectangle;
        cellRectangleIds[1] = R.drawable.cell_rectangle_2;
        cellRectangleIds[2] = R.drawable.cell_rectangle_4;
        cellRectangleIds[3] = R.drawable.cell_rectangle_8;
        cellRectangleIds[4] = R.drawable.cell_rectangle_16;
        cellRectangleIds[5] = R.drawable.cell_rectangle_32;
        cellRectangleIds[6] = R.drawable.cell_rectangle_64;
        cellRectangleIds[7] = R.drawable.cell_rectangle_128;
        cellRectangleIds[8] = R.drawable.cell_rectangle_256;
        cellRectangleIds[9] = R.drawable.cell_rectangle_512;
        cellRectangleIds[10] = R.drawable.cell_rectangle_1024;
        cellRectangleIds[11] = R.drawable.cell_rectangle_2048;
        return cellRectangleIds;
    }

    private void createOverlays() {
        Resources resources = getResources();
        Bitmap bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        lightUpRectangle.setAlpha(127);
        drawDrawable(canvas, lightUpRectangle, 0, 0, bitmap.getWidth(), getHeight());
        lightUpRectangle.setAlpha(255);
        winGameOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        fadeRectangle.setAlpha(127);
        drawDrawable(canvas, fadeRectangle, 0, 0, bitmap.getWidth(), getHeight());
        fadeRectangle.setAlpha(255);
        loseGameOverlay = new BitmapDrawable(resources, bitmap);
    }

    private void tick() {
        long currentTime = System.nanoTime();
        game.aGrid.tickAll(currentTime - lastFPSTime);
        lastFPSTime = currentTime;
    }

    public void resyncTime() {
        lastFPSTime = System.nanoTime();
    }

    private void getLayout(int width, int height) {
        cellSize = Math.min(width / (game.getFieldSize() + 1), height / (game.getFieldSize() + 1));
        gridWidth = cellSize / 7;
        int screenMiddleX = width / 2;
        int screenMiddleY = height / 2;

        // размеры сетки
        double halfNumSquaresX = game.getFieldSize() / 2d;
        double halfNumSquaresY = game.getFieldSize() / 2d;
        startingX = (int) (screenMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2);
        endingX = (int) (screenMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2);
        startingY = (int) (screenMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2);
        endingY = (int) (screenMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2);
        resyncTime();
    }
}