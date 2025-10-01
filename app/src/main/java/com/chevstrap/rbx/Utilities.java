package com.bloxstrap.client;

import android.util.Log;

public class Utilities {
    private static final String TAG = "Utilities";

    // just a simple little function to run shell commands
    public static Process ShellExecute(String[] cmd) {
        try {
            return Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            Log.e(TAG, "oops, shell command failed", e);
            return null;
        }
    }
}
