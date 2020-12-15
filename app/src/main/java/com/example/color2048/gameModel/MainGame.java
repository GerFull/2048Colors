package com.example.color2048.gameModel;

import android.content.Context;

import com.example.color2048.GameView;
import com.example.color2048.utility.DebugTools;
import com.example.color2048.utility.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainGame {

    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public boolean isEndDialogShown() {
        return isEndDialogShown;
    }

    public void setEndDialogShown(boolean endDialogShown) {
        isEndDialogShown = endDialogShown;
    }

    private boolean isEndDialogShown = false;

    public static final int FADE_GLOBAL_ANIMATION = 0;
    private static final long MOVE_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME;
    private static final long SPAWN_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME;
    private static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final long NOTIFICATION_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME * 5;
    private static final int startingMaxValue = 2048;

    private static final int GAME_WIN = 1;
    private static final int GAME_LOST = -1;
    private static final int GAME_NORMAL = 0;
    public int gameState = GAME_NORMAL;
    public int lastGameState = GAME_NORMAL;
    private int bufferGameState = GAME_NORMAL;

    public int getFieldSize() {
        return fieldSize;
    }

    private final int fieldSize;
    private final Context mContext;
    private final GameView mView;
    public Grid grid = null;
    public AnimationGrid aGrid;
    public boolean canUndo;
    public long score = 0;
    public long highScore = 0;
    public long lastScore = 0;
    private long bufferScore = 0;

    public MainGame(Context context, GameView view, int size) {
        mContext = context;
        mView = view;
        this.fieldSize = size;
    }

    public void newGame() {
        isEndDialogShown = false;
        if (grid == null)
            grid = new Grid(fieldSize, fieldSize);
        else {
            prepareUndoState();
            saveUndoState();
            grid.clearGrid();
        }
        aGrid = new AnimationGrid(fieldSize, fieldSize);
        highScore = Utils.getHighScore(mContext,fieldSize);
        if (score >= highScore) {
            highScore = score;
            Utils.saveHighScore(mContext,fieldSize,highScore);
        }
        score = DebugTools.getStartingScore();
        gameState = GAME_NORMAL;
        addStartTiles();
        mView.refreshLastTime = true;
        mView.resyncTime();
        mView.invalidate();
    }

    private void addStartTiles() {
        List<Tile> debugTiles = DebugTools.generatePremadeMap();
        if (debugTiles != null) {
            for (Tile tile : debugTiles)
                this.spawnTile(tile);
            return;
        }

        int startTiles = 2;
        for (int xx = 0; xx < startTiles; xx++)
            this.addRandomTile();
    }

    private void addRandomTile() {
        if (grid.isCellsAvailable()) {
            int value = Math.random() < 0.9 ? 2 : 4;
            Tile tile = new Tile(grid.randomAvailableCell(), value);
            spawnTile(tile);
        }
    }

    private void spawnTile(Tile tile) {
        grid.insertTile(tile);
        aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);
    }

    private void prepareTiles() {
        for (Tile[] array : grid.field)
            for (Tile tile : array)
                if (grid.isCellOccupied(tile))
                    tile.setMergedFrom(null);
    }

    private void moveTile(Tile tile, Cell cell) {
        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState() {
        grid.saveTiles();
        canUndo = true;
        lastScore = bufferScore;
        lastGameState = bufferGameState;
    }

    private void prepareUndoState() {
        grid.prepareSaveTiles();
        bufferScore = score;
        bufferGameState = gameState;
    }

    public void revertUndoState() {
        if (canUndo) {
            canUndo = false;
            aGrid.cancelAnimations();
            grid.revertTiles();
            score = lastScore;
            gameState = lastGameState;
            mView.refreshLastTime = true;
            mView.invalidate();
        }
    }

    public boolean gameWon() {
        return (gameState == GAME_WIN);
    }


    public boolean gameLost() {
        return (gameState == GAME_LOST);
    }

    public boolean isActive() {
        return !(gameWon() || gameLost());
    }

    public void move(int direction) {
        aGrid.cancelAnimations();
        // 0: вверх, 1: вправо, 2: вниз, 3: влево
        if (!isActive())
            return;
        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;
        prepareTiles();
        for (int xx : traversalsX) {
            for (int yy : traversalsY) {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        // объединить позиции двух ячеек
                        tile.updatePosition(positions[1]);

                        int[] extras = {xx, yy};
                        aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras); //Direction: 0 = двигаются соединенные
                        aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        score = score + merged.getValue();
                        highScore = Math.max(score, highScore);
                        if (score >= highScore) {
                            highScore = score;
                            Utils.saveHighScore(mContext,fieldSize,highScore);
                        }

                        if (merged.getValue() >= winValue() && !gameWon()) {
                            gameState = GAME_WIN;  //ПОБЕДА
                            endGame();
                        }
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); //Direction: 1 = MOVING NO MERGE
                    }
                    if (!positionsEqual(cell, tile))
                        moved = true;
                }
            }
        }
        if (moved) {
            saveUndoState();
            addRandomTile();
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();
    }

    private void checkLose() {
        if (!movesAvailable() && !gameWon()) {
            gameState = GAME_LOST;
            endGame();
        }
    }

    private void endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore) {
            highScore = score;
            Utils.saveHighScore(mContext,fieldSize,highScore);
        }
    }

    private Cell getVector(int direction) {
        Cell[] map = {
                new Cell(0, -1), // вверх
                new Cell(1, 0),  // вправо
                new Cell(0, 1),  // вниз
                new Cell(-1, 0)  // влево
        };
        return map[direction];
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<>();
        for (int xx = 0; xx < fieldSize; xx++)
            traversals.add(xx);
        if (vector.getX() == 1)
            Collections.reverse(traversals);
        return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<>();
        for (int xx = 0; xx < fieldSize; xx++)
            traversals.add(xx);
        if (vector.getY() == 1)
            Collections.reverse(traversals);
        return traversals;
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));
        return new Cell[]{previous, nextCell};
    }

    private boolean movesAvailable() {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;
        for (int xx = 0; xx < fieldSize; xx++) {
            for (int yy = 0; yy < fieldSize; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));
                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());
                        Tile other = grid.getCellContent(cell);
                        if (other != null && other.getValue() == tile.getValue())
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean positionsEqual(Cell first, Cell second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    private int winValue() {
        return startingMaxValue;
    }
}
