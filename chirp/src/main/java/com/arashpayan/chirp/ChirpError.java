package com.arashpayan.chirp;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/5/16.
 */
class ChirpError {

    protected String mCause;

    public ChirpError(String cause) {
        this.mCause = cause;
    }

    @Override
    public String toString() {
        return mCause;
    }
}
