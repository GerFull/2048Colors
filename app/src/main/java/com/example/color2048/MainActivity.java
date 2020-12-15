package com.example.color2048;

import android.content.Intent;
import android.os.Bundle;

import com.example.color2048.utility.Utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayoutCompat linearLayout4by4=findViewById(R.id.game4by4);
        LinearLayoutCompat linearLayout5by5=findViewById(R.id.game5by5);
        LinearLayoutCompat linearLayout6by6=findViewById(R.id.game6by6);
        LinearLayoutCompat linearLayout8by8=findViewById(R.id.game8by8);
        linearLayout4by4.setOnClickListener(this);
        linearLayout5by5.setOnClickListener(this);
        linearLayout6by6.setOnClickListener(this);
        linearLayout8by8.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.game4by4:
                launchGame(4);
                break;
            case R.id.game5by5:
                launchGame(5);
                break;
            case R.id.game6by6:
                launchGame(6);
                break;
            case R.id.game8by8:
                launchGame(8);
                break;
        }

    }

    private void launchGame(int fieldSize){
        Intent intent = new Intent(this,GameActivity.class);
        intent.putExtra(Utils.INTENT_FIELD_SIZE,fieldSize);
        startActivity(intent);
    }
}