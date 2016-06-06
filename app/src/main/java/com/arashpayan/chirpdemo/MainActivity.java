package com.arashpayan.chirpdemo;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.arashpayan.chirp.Chirp;
import com.arashpayan.chirp.ChirpBrowser;
import com.arashpayan.chirp.ChirpBrowserListener;
import com.arashpayan.chirp.ChirpPublisher;
import com.arashpayan.chirp.Service;

public class MainActivity extends AppCompatActivity implements ChirpBrowserListener {

    public static final String TAG = "ChirpDemo";
    private ChirpBrowser mChirpBrowser;
    private ChirpPublisher mChirpPublisher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mChirpBrowser = Chirp.browseFor("*").
//                              listener(this).
//                              start(getApplication());

        mChirpPublisher = Chirp.publish("wqwq").
                                start(getApplication());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mChirpBrowser != null) {
            mChirpBrowser.stop();
        }
        if (mChirpPublisher != null) {
            Log.i(TAG, "onDestroy: calling publisher stop");
            mChirpPublisher.stop();
        }
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
