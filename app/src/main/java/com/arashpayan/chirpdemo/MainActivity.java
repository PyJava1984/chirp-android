package com.arashpayan.chirpdemo;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.arashpayan.chirp.Chirp;
import com.arashpayan.chirp.ChirpBrowser;
import com.arashpayan.chirp.ChirpBrowserListener;
import com.arashpayan.chirp.Service;

public class MainActivity extends AppCompatActivity implements ChirpBrowserListener {

    public static final String TAG = "ChirpDemo";
    private ChirpBrowser mChirpBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mChirpBrowser = Chirp.browseFor("*").
                              listener(this).
                              start(getApplication());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mChirpBrowser.isStarted()) {
            mChirpBrowser.start(getApplication());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mChirpBrowser.stop();
    }

    @Override
    public void onServiceDiscovered(@NonNull Service service) {
        Log.i(TAG, "onServiceDiscovered: " + service);
    }

    @Override
    public void onServiceUpdated(@NonNull Service service) {
        Log.i(TAG, "onServiceUpdated: " + service);
    }

    @Override
    public void onServiceRemoved(@NonNull Service service) {
        Log.i(TAG, "onServiceRemoved: " + service);
    }
}
