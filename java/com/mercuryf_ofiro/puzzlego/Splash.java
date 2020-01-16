package com.mercuryf_ofiro.puzzlego;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Splash extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(() -> {
            /* Create an Intent that will start the Menu-Activity. */
            Intent mainIntent = new Intent(Splash.this, MapsActivity.class);
            Splash.this.startActivity(mainIntent);
            Splash.this.finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}
