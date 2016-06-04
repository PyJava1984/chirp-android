package com.arashpayan.chirp;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Arash Payan (https://arashpayan.com) on 5/14/16.
 */
public class Chirp {

    public static final int MAX_MSG_LENGTH = 64 * 1024;
    private static final Pattern sServiceNamePattern = Pattern.compile("[a-zA-Z0-9\\.\\-]+");
    private static final SecureRandom sSecureRandom = new SecureRandom();
    protected static final char[] sHexArray = "0123456789abcdef".toCharArray();

    private Chirp() {
    }

    public static ChirpBrowser.Builder browseFor(@NonNull String serviceName) {
        return new ChirpBrowser.Builder(serviceName);
    }

    protected static String getRandomId() {
        byte[] randData = new byte[16];
        sSecureRandom.nextBytes(randData);

        char[] hexChars = new char[16 * 2];
        for ( int j = 0; j < 16; j++ ) {
            int v = randData[j] & 0xFF;
            hexChars[j * 2] = sHexArray[v >>> 4];
            hexChars[j * 2 + 1] = sHexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static boolean isValidServiceName(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }

        if (name.getBytes().length > 64) {
            return false;
        }

        Matcher matcher = sServiceNamePattern.matcher(name);
        return matcher.matches();
    }

}
