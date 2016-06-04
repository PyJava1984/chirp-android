package com.arashpayan.chirp;

import android.support.annotation.NonNull;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/2/16.
 */
public interface ChirpBrowserListener {

    void onServiceDiscovered(@NonNull Service service);
    void onServiceUpdated(@NonNull Service service);
    void onServiceRemoved(@NonNull Service service);

}
