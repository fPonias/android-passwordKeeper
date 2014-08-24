package com.munger.passwordkeeper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends Activity 
{
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 2500;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_splashscreen);
 
        new Handler().postDelayed(new Runnable() {public void run() 
        {
            Intent i = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(i);
 
            finish();
        }}, SPLASH_TIME_OUT);
    }
}
