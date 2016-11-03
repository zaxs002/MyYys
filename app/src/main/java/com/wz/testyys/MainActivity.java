package com.wz.testyys;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private boolean isServiceRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSharedPreferences("Game", MODE_PRIVATE).edit().putInt("width", GameUtils.getScreenMetrix(this).widthPixels).commit();
        getSharedPreferences("Game", MODE_PRIVATE).edit().putInt("height", GameUtils.getScreenMetrix(this).heightPixels).commit();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        System.out.println("x: " + event.getRawX() + " y: " + event.getRawY());
        return super.onTouchEvent(event);
    }

    public void start(View view) {
        isServiceRunning = !isServiceRunning;
        if (isServiceRunning) {
            DisplayMetrics screenMetrix = ScreenUtils.getScreenMetrix(this);
            int width = screenMetrix.widthPixels;
            int height = screenMetrix.heightPixels;
            Intent gameIntent = new Intent(this, GameService.class);
            gameIntent.putExtra("width", width);
            gameIntent.putExtra("height", height);
            startService(gameIntent);
        } else {
            Intent gameIntent = new Intent(this, GameService.class);
            stopService(gameIntent);
        }
    }
}
