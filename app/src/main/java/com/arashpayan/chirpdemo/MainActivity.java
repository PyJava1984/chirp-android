package com.arashpayan.chirpdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.arashpayan.chirp.Chirp;

public class MainActivity extends AppCompatActivity {

    private Chirp mChirp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mChirp = new Chirp();
        mChirp.listen(getApplication());
    }
}
