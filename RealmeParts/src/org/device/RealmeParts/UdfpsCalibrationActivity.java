package org.device.RealmeParts;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class UdfpsCalibrationActivity extends Activity {

    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private TextView mInstructionText;
    private Button mStartButton;
    
    private float mCurrentLux = 0f;
    private float mMaxLux = 0f;
    private boolean mIsCalibrating = false;
    private float mOriginalBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    
    private int mCalibrationPhase = 0; // 0=idle, 1=brightening, 2=dimming
    private ArrayList<Float> mLuxHistoryUp = new ArrayList<>();
    private ArrayList<Float> mLuxHistoryDown = new ArrayList<>();

    private SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mCurrentLux = event.values[0];
            if (mCalibrationPhase == 1) {
                mLuxHistoryUp.add(mCurrentLux);
                if (mCurrentLux > mMaxLux) {
                    mMaxLux = mCurrentLux;
                }
            } else if (mCalibrationPhase == 2) {
                mLuxHistoryDown.add(mCurrentLux);
            } else {
                updateUIForEnvironment();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        View layout = new View(this);
        layout.setBackgroundColor(Color.BLACK);
        
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        
        mInstructionText = new TextView(this);
        mInstructionText.setText("Initializing light sensor...");
        mInstructionText.setTextColor(Color.WHITE);
        mInstructionText.setTextSize(18f);
        mInstructionText.setGravity(android.view.Gravity.CENTER);
        
        mStartButton = new Button(this);
        mStartButton.setText("Start Calibration");
        mStartButton.setEnabled(false);
        mStartButton.setOnClickListener(v -> startCalibration());
        
        root.addView(mInstructionText);
        root.addView(mStartButton);
        
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null && mLightSensor != null) {
            mSensorManager.registerListener(mLightSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mLightSensorListener);
        }
        if (mIsCalibrating) {
            finishCalibration(false);
        }
    }

    private boolean mFinished = false;

    private void updateUIForEnvironment() {
        if (mFinished) return;
        if (mCurrentLux > 5.0f) {
            mInstructionText.setText("Environment too bright (" + mCurrentLux + " lux).\nPlease move to a pitch black room to calibrate screen bleed.");
            mStartButton.setEnabled(false);
        } else {
            mInstructionText.setText("Environment looks dark (" + mCurrentLux + " lux).\nReady to calibrate. The screen will turn full white and maximum brightness for 3 seconds.");
            mStartButton.setEnabled(true);
        }
    }

    private void startCalibration() {
        mIsCalibrating = true;
        mCalibrationPhase = 1;
        mMaxLux = 0f;
        mLuxHistoryUp.clear();
        mLuxHistoryDown.clear();
        mStartButton.setEnabled(false);
        mInstructionText.setTextColor(Color.BLACK);
        mInstructionText.setText("Calibrating Brightening... Please wait.");
        
        // Make screen white and max brightness
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(Color.WHITE);
        }
        
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        mOriginalBrightness = lp.screenBrightness;
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
        
        // Wait 3 seconds to gather peak lux and alpha_up
        new Handler(Looper.getMainLooper()).postDelayed(() -> startDimmingPhase(), 3000);
    }
    
    private void startDimmingPhase() {
        mCalibrationPhase = 2;
        mInstructionText.setTextColor(Color.WHITE);
        mInstructionText.setText("Calibrating Dimming... Please wait.");
        
        // Make screen black and 0 brightness
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(Color.BLACK);
        }
        
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.0f;
        getWindow().setAttributes(lp);
        
        // Wait 3 seconds to gather alpha_down
        new Handler(Looper.getMainLooper()).postDelayed(() -> finishCalibration(true), 3000);
    }

    private void finishCalibration(boolean success) {
        mIsCalibrating = false;
        
        // Restore brightness and background
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = mOriginalBrightness;
        getWindow().setAttributes(lp);
        
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(Color.TRANSPARENT);
        }
        
        mInstructionText.setTextColor(Color.WHITE);
        
        if (success) {
            // Save the max bleed threshold
            int threshold = (int) Math.ceil(mMaxLux);
            // Add a small safety margin
            threshold = Math.max(10, threshold + 10);
            SystemProperties.set("persist.sys.udfps.lux_threshold", String.valueOf(threshold));
            
            // Calculate alpha_up (exact mathematical alpha based on the crossing event)
            float alphaUp = 0.2f;
            float targetLuxUp = mMaxLux * 0.632f;
            for (int i = 0; i < mLuxHistoryUp.size(); i++) {
                float v = mLuxHistoryUp.get(i);
                if (v >= targetLuxUp) {
                    int n = i + 1;
                    float ratio = v / mMaxLux;
                    // Formula: alpha = 1 - (1 - ratio)^(1/n)
                    alphaUp = 1.0f - (float) Math.pow(1.0 - ratio, 1.0 / n);
                    break;
                }
            }
            alphaUp = Math.max(0.01f, Math.min(1.0f, alphaUp));
            SystemProperties.set("persist.sys.udfps.alpha_up", String.valueOf(alphaUp));
            
            // Calculate alpha_down (exact mathematical alpha based on the crossing event)
            float alphaDown = 0.2f;
            float targetLuxDown = mMaxLux * 0.368f;
            boolean foundDown = false;
            for (int i = 0; i < mLuxHistoryDown.size(); i++) {
                float v = mLuxHistoryDown.get(i);
                if (v <= targetLuxDown) {
                    int n = i + 1;
                    float ratio = v / mMaxLux;
                    // Formula: alpha = 1 - (ratio)^(1/n)
                    if (ratio <= 0.0f) {
                        alphaDown = 1.0f;
                    } else {
                        alphaDown = 1.0f - (float) Math.pow(ratio, 1.0 / n);
                    }
                    foundDown = true;
                    break;
                }
            }
            if (!foundDown && !mLuxHistoryDown.isEmpty()) {
                int n = mLuxHistoryDown.size();
                float v = mLuxHistoryDown.get(n - 1);
                float ratio = v / mMaxLux;
                if (ratio <= 0.0f) {
                    alphaDown = 1.0f;
                } else {
                    alphaDown = 1.0f - (float) Math.pow(ratio, 1.0 / n);
                }
            }
            alphaDown = Math.max(0.01f, Math.min(1.0f, alphaDown));
            SystemProperties.set("persist.sys.udfps.alpha_down", String.valueOf(alphaDown));
            
            mFinished = true;
            mInstructionText.setText("Calibration complete!\n\nMax Bleed: " + threshold + " lux\nAlpha Up: " + alphaUp + "\nAlpha Down: " + alphaDown);
            mStartButton.setText("Done");
            mStartButton.setEnabled(true);
            mStartButton.setOnClickListener(v -> finish());
        } else {
            updateUIForEnvironment();
        }
    }
}
