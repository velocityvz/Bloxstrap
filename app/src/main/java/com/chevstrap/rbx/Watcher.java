package com.bloxstrap.client;

import static com.bloxstrap.client.Utility.INeedPath.getRBXPathDir;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.bloxstrap.client.Integrations.ActivityWatcher;
import com.bloxstrap.client.Utility.AppCheckerListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// okay so this class is basically a little spy, it watches for when roblox opens and closes!
public class Watcher {
    private static Watcher currentInstance;
    private ExecutorService RBXActivityWatcher;
    private AppCheckerListener checker;

    public void run(Context context) {
        // if we already have a watcher running, we should probably stop it first, lol
        if (currentInstance != null) {
            currentInstance.stop();
        }
        currentInstance = this;

        checker = new AppCheckerListener(context, context.getPackageName(), new AppCheckerListener.Listener() {
            @Override
            public void onAppStarted() {
                FFlagsSettingsManager manager = new FFlagsSettingsManager(context);

                // make sure the settings are actually enabled before we do anything
                boolean getSetting1 = Boolean.parseBoolean(manager.getSetting("EnableActivityTracking"));
                boolean getSetting2 = Boolean.parseBoolean(manager.getSetting("ShowServerDetails"));

                if (!getSetting1 || !getSetting2) return;

                stopExecutorService(); // gotta stop the old one just in case!

                RBXActivityWatcher = Executors.newSingleThreadExecutor();
                String target = FFlagsSettingsManager.getPackageTarget(context);

                String rbxpath = getRBXPathDir(context, target);
                if (rbxpath == null) {
                    Toast.makeText(context, "Roblox path is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                // time to start watching the roblox logs hehe
                ActivityWatcher watcher = new ActivityWatcher(context, rbxpath.concat("appData/logs"), RBXActivityWatcher);
                RBXActivityWatcher.submit(watcher::start);
                Log.d("AppChecker", "roblox started, watching now...");
            }

            @Override
            public void onAppStopped() {
                // roblox closed, so we can just chill and stop watching
                stopExecutorService();
                Log.d("AppChecker", "roblox stopped.");
            }
        });

        checker.start();
    }

    private void stop() {
        if (checker != null) {
            checker.stop();
        }
        stopExecutorService();
    }

    private void stopExecutorService() {
        if (RBXActivityWatcher != null && !RBXActivityWatcher.isShutdown()) {
            RBXActivityWatcher.shutdownNow();
        }
    }
}
