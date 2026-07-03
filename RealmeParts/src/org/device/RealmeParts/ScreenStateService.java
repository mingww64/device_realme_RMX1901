package org.device.RealmeParts;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.device.RealmeParts.Touch.util.Utils;

public class ScreenStateService extends Service {
    private static final String TAG = "ScreenStateService";
    private static final String FP_ENABLE_PATH = "/proc/touchpanel/fp_enable";
    private static final String DT2W_ENABLE_PATH = "/proc/touchpanel/double_tap_enable";
    private static final String SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled";

    private boolean mWasFpEnabled = false;

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

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            String action = intent.getAction();
            boolean isScreenOffUdfpsEnabled = Settings.Secure.getInt(
                    context.getContentResolver(), SCREEN_OFF_UDFPS_ENABLED, 0) == 1;

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (!isScreenOffUdfpsEnabled) {
                    String fpState = Utils.getFileValue(FP_ENABLE_PATH, "0");
                    if ("1".equals(fpState)) {
                        mWasFpEnabled = true;
                        Utils.writeValue(FP_ENABLE_PATH, "0");
                        Log.d(TAG, "Screen off: Disabled fp_enable because screen-off UDFPS is disabled in Settings.");
                    } else {
                        mWasFpEnabled = false;
                    }
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (!isScreenOffUdfpsEnabled && mWasFpEnabled) {
                    Utils.writeValue(FP_ENABLE_PATH, "1");
                    mWasFpEnabled = false;
                    Log.d(TAG, "Screen on: Restored fp_enable.");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenStateReceiver, filter);

        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE),
                false, mDt2wObserver);
        updateDt2wState();

        Log.d(TAG, "ScreenStateService created and receivers registered.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        getContentResolver().unregisterContentObserver(mDt2wObserver);
        Log.d(TAG, "ScreenStateService destroyed and receivers unregistered.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
