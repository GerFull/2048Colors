package com.example.color2048.utility;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utils {

    public static final String SP_HIGHSCORE = "highscore";
    public static final String INTENT_FIELD_SIZE = "fieldSize";

    public static Activity getActivity(Context context) {
        while (!(context instanceof Activity)) {
            if (!(context instanceof ContextWrapper))
                context = null;
            ContextWrapper contextWrapper = (ContextWrapper) context;
            if (contextWrapper == null)
                return null;
            context = contextWrapper.getBaseContext();
            if (context == null)
                return null;
        }
        return (Activity) context;
    }

    public static long getHighScore(Context c, int fieldSize) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getLong(SP_HIGHSCORE + fieldSize, -1);
    }

    public static void saveHighScore(Context c, int fieldSize, long highscore) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        sp.edit().putLong(SP_HIGHSCORE + fieldSize, highscore).apply();
    }
}
