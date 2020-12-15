package com.example.color2048;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.color2048.gameModel.MainGame;
import com.example.color2048.utility.Utils;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    private Context c;
    private MainGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = this;
        setContentView(R.layout.activity_game);

        int fieldSize =4;

        if(getIntent().getExtras()!=null)
            fieldSize=getIntent().getIntExtra(Utils.INTENT_FIELD_SIZE,4);

        AppCompatTextView textViewScore = findViewById(R.id.textViewScore);
        AppCompatTextView textViewHighScore = findViewById(R.id.textViewHighScore);
        AppCompatImageButton buttonRefresh = findViewById(R.id.buttonRefresh);
        AppCompatImageButton buttonUndo = findViewById(R.id.buttonUndo);
        GameView gameView = findViewById(R.id.gameView);
        game = new MainGame(this, gameView,fieldSize);
        game.newGame();
        gameView.init(this,game, textViewScore, textViewHighScore);
        buttonRefresh.setOnClickListener(this);
        buttonUndo.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.buttonRefresh:
                new AlertDialog.Builder(c)
                        .setPositiveButton(R.string.reset, (dialog, which) -> game.newGame())
                        .setNegativeButton(R.string.continue_game, null)
                        .setTitle(R.string.reset_dialog_title)
                        .setMessage(R.string.reset_dialog_message)
                        .show();
                break;
            case R.id.buttonUndo:
                game.revertUndoState();
                break;
        }
    }
}
