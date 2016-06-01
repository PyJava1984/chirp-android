package com.arashpayan.chirp;

import android.util.Log;

/**
 * Created by arash on 5/31/16.
 */
public class ChirpLog {

    private static final String TAG = "Chirp";

    protected static void logi(String msg) {
        Log.i(TAG, msg);
    }

    protected static void logw(String msg, Throwable t) {
        Log.w(TAG, msg, t);
    }

}
