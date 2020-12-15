package com.example.color2048.utility;


import androidx.annotation.Nullable;

import com.example.color2048.gameModel.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 для удобства дебага: DEBUG_ENABLED = true, чтобы загрузить демо-карту
 */
public class DebugTools {
    private static final boolean DEBUG_ENABLED = false;

    private static final int[][] DEMO_MAP = {
        {0,2,4,8},
        {16,32, 64, 128,256},
        {512,1024, 0,  0},
        {0,0, 0, 1024, 0},
    };
    private static final long STARTING_SCORE = 22235;

    @Nullable
    public static List<Tile> generatePremadeMap() {
        if (!DEBUG_ENABLED)
            return null;

        if (DEMO_MAP == null)
            return null;

        List<Tile> result = new ArrayList<>();
        for (int yy = 0; yy < DEMO_MAP.length; yy++) {
            for (int xx = 0; xx < DEMO_MAP[0].length; xx++) {
                if (DEMO_MAP[yy][xx] == 0)
                    continue;

                result.add(new Tile(xx, yy, DEMO_MAP[yy][xx]));
            }
        }

        return result;
    }

    public static long getStartingScore() {
        if (!DEBUG_ENABLED)
            return 0;
        return STARTING_SCORE;
    }
}
