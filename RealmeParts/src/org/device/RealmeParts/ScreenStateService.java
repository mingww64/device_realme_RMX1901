package org.device.RealmeParts;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.device.RealmeParts.Touch.util.Utils;

public class ScreenStateService extends Service {
    private static final String TAG = "ScreenStateService";
    private static final String DT2W_ENABLE_PATH = "/proc/touchpanel/double_tap_enable";

    private ContentObserver mDt2wObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            updateDt2wState();
        }
    };

    private void updateDt2wState() {
        boolean isDt2wEnabled = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.DOUBLE_TAP_TO_WAKE, 1) == 1;
        Utils.writeValue(DT2W_ENABLE_PATH, isDt2wEnabled ? "1" : "0");
        Log.d(TAG, "DT2W state updated to: " + isDt2wEnabled);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE),
                false, mDt2wObserver);
        updateDt2wState();

        Log.d(TAG, "ScreenStateService created; synchronizing Double Tap to Wake.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mDt2wObserver);
        Log.d(TAG, "ScreenStateService destroyed and receivers unregistered.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
