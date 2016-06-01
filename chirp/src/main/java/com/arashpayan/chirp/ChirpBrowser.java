package com.arashpayan.chirp;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.arashpayan.chirp.ChirpLog.logw;

/**
 * Created by arash on 5/31/16.
 */
public class ChirpBrowser {

    private final String mServiceName;
    private ExecutorService mExecutor;
    private boolean mIsStarted;

    public ChirpBrowser(String serviceName) {
        if (!Chirp.isValidServiceName(serviceName)) {
            throw new IllegalArgumentException("Invalid service name");
        }
        mServiceName = serviceName;
    }

    private void listen4() {
        try {

        } catch (Throwable t) {
            logw("listen4: ", t);
        }
    }

    public ChirpBrowser start() {
        mExecutor = Executors.newScheduledThreadPool(2);

        return this;
    }

}
