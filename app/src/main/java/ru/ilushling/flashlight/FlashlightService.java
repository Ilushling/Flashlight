package ru.ilushling.flashlight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlashlightService extends Service {

    String TAG = "Service";
    public static final String NOTIFICATION_CHANNEL_ID = "ilushling.flashlight", NOTIFICATION_CHANNEL_NAME = "screenfilter";

    BroadcastReceiver mReceiver;

    private Camera camera;
    private Parameters params;
    private CameraManager mCameraManager;
    private String mCameraId;
    private boolean isCameraGet = false, isFlash, isFlashSos;
    int buttonPowercount, flashSosTimer;
    boolean powerButtonTaskfirst;
    List<String> flashMode;
    public static final int ID_SERVICE = 99543;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Screen listener
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new MyReceiver();
        registerReceiver(mReceiver, filter);

        startNotification();
    }

    void startNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // Notification Switch
            // Prepare
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            Intent intentSwitch = new Intent(this, MyReceiver.class);
            intentSwitch.setAction(MyReceiver.ACTION_SWITCH);
            PendingIntent pIntentSwitch = PendingIntent.getBroadcast(this, 0, intentSwitch, PendingIntent.FLAG_UPDATE_CURRENT);
            // Notification Close
            Intent intentServiceOff = new Intent(this, MyReceiver.class);
            intentServiceOff.setAction(MyReceiver.ACTION_SERVICE_OFF);
            PendingIntent pIntentServiceOff = PendingIntent.getBroadcast(this, 0, intentServiceOff, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder notification;
            // Build notification
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (mNotificationManager != null) {
                    int importance = NotificationManager.IMPORTANCE_MIN;
                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
                    mNotificationManager.createNotificationChannel(notificationChannel);
                }
                notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getString(R.string.app_name))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pIntentSwitch)
                        .setContentText(getString(R.string.touch_to_switch))
                        .addAction(R.mipmap.ic_launcher, getString(R.string.turn_off), pIntentServiceOff);
            } else {
                notification = new Notification.Builder(this)
                        .setContentTitle(getString(R.string.app_name))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pIntentSwitch)
                        .setContentText(getString(R.string.touch_to_switch))
                        .addAction(R.mipmap.ic_launcher, getString(R.string.turn_off), pIntentServiceOff);
            }

            if (mNotificationManager != null) {
                mNotificationManager.notify(ID_SERVICE, notification.build());
                startForeground(ID_SERVICE, notification.build());
            }
        }
    }

    private void turnOnSos() {
        isFlashSos = true;

        if (!isCameraGet) {
            getCamera();
        }

        flashSosTimer = 0;
        flashSosTimerTask.run();
    }

    // Get the camera
    private void getCamera() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mCameraManager == null) {
                    mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    if (mCameraManager != null) {
                        mCameraId = mCameraManager.getCameraIdList()[0];
                    }
                }
            } else {
                if (camera == null) {
                    // Open
                    camera = Camera.open();
                    params = camera.getParameters();
                    flashMode = params.getSupportedFlashModes();
                }
            }

            isCameraGet = true;
        } catch (Exception e) {
            isCameraGet = false;
            isFlash = false;
            isFlashSos = false;
            Log.e(TAG, "GetCamera: " + e.getMessage());


            Bundle bundle = new Bundle();
            bundle.putString("error", e + "");
            mFirebaseAnalytics.logEvent("error_getCamera", bundle);
        }
    }

    // TURN ON FLASH
    private void turnOnFlash(String method) {
        method = method != null ? method : "";
        try {
            //Log.e(TAG, "Service 1");
            isFlash = true;
            // UI
            if (!method.equals("sos")) {
                updateUI.run();
            }

            // Getting camera too long for UI
            if (!isCameraGet) {
                getCamera();
            }

            //Log.e(TAG, "Service 2");
            // Turn on Flash
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, true);
            } else {
                if (camera != null) {
                    if (flashMode.contains(Parameters.FLASH_MODE_TORCH)) {
                        params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(params);
                    } else {
                        if (flashMode.contains(Parameters.FLASH_MODE_ON)) {
                            params.setFlashMode(Parameters.FLASH_MODE_ON);
                        } else if (flashMode.contains(Parameters.FLASH_MODE_AUTO)) {
                            params.setFlashMode(Parameters.FLASH_MODE_AUTO);
                        } else if (flashMode.contains(Parameters.FLASH_MODE_RED_EYE)) {
                            params.setFlashMode(Parameters.FLASH_MODE_RED_EYE);
                        } else {
                            isFlash = false;
                            updateUI.run();
                        }
                        if (Build.VERSION.SDK_INT > 10) {
                            try {
                                camera.setPreviewTexture(new SurfaceTexture(0));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        camera.setParameters(params);
                        camera.startPreview();
                        camera.autoFocus(null);
                    }
                }
            }

            Bundle bundle = new Bundle();
            ArrayList allWords = new ArrayList<>(params.getSupportedFlashModes());
            bundle.putStringArrayList("flashModes", allWords);
        } catch (Exception e) {
            Log.e(TAG, "Failed turn on: " + e.getMessage());
            Bundle bundle = new Bundle();
            bundle.putString("error", e + "");
            ArrayList allWords = new ArrayList<>(params.getSupportedFlashModes());
            bundle.putStringArrayList("errorFlashModes", allWords);
            mFirebaseAnalytics.logEvent("error_turn_on", bundle);

            turnOffFlash("");
        }
    }

    // Power button zeroing counter
    /*
     * First run of handle start timer via delay for few milliseconds
     * Second run zeroing timer to 0
     */
    private Handler powerButtonHandler = new Handler();

    // TURN OFF FLASH
    private void turnOffFlash(String method) {
        try {
            // Update UI
            isFlash = false;
            updateUI.run();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, false);
                if (!method.equals("sos")) {
                    flashSosTimingHandler.removeCallbacks(flashSosTimerTask);

                    mCameraManager = null;
                    mCameraId = null;

                    isCameraGet = false;
                    isFlash = false;
                    isFlashSos = false;
                }
                // Update UI
                isFlash = false;
                updateUI.run();
            } else {
                if (camera != null) {
                    params.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                    if (!method.equals("sos")) {
                        flashSosTimingHandler.removeCallbacks(flashSosTimerTask);

                        params.setFlashMode(Parameters.FLASH_MODE_OFF);
                        camera.setParameters(params);
                        camera.stopPreview();
                        camera.release();
                        camera = null;

                        isCameraGet = false;
                        isFlash = false;
                        isFlashSos = false;
                    }
                    // Update UI
                    isFlash = false;
                    updateUI.run();
                }
            }

            //Log.e(TAG, "turn off");
        } catch (Exception e) {
            Log.e(TAG, "Failed turn off flash: " + e.getMessage());

            Bundle bundle = new Bundle();
            bundle.putString("error", e + "");
            mFirebaseAnalytics.logEvent("error_turn_off", bundle);

            // Update UI
            turnOffFlash("");
        }
    }

    private Runnable updateUI = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FlashlightService.this, MyReceiver.class);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.setAction(MyReceiver.ACTION_IS_FLASH);
            intent.putExtra("isFlash", isFlash);
            sendBroadcast(intent);
        }
    };
    private Runnable powerButtonTask = new Runnable() {
        @Override
        public void run() {
            //Log.e(TAG, "timer: " + taskfirst);
            if (!powerButtonTaskfirst) {
                powerButtonHandler.postDelayed(powerButtonTask, 1500);
                powerButtonTaskfirst = true;
            } else {
                buttonPowercount = 0;
                powerButtonTaskfirst = false;
            }

        }
    };
    // FlashSos timing
    private Handler flashSosTimingHandler = new Handler();
    private Runnable flashSosTimerTask = new Runnable() {
        @Override
        public void run() {

            try {
                // Timing SOS (3 short 3 long 3 short)
                switch (flashSosTimer) {
                    // [START SHORT]
                    case 0:
                        // FIRST ON
                        flashSosTimingHandler.postDelayed(this, 500);
                        break;
                    case 1:
                        // OFF
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    case 2:
                        flashSosTimingHandler.postDelayed(this, 500);
                        break;
                    case 3:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    case 4:
                        flashSosTimingHandler.postDelayed(this, 500);
                        break;
                    case 5:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    // [END short]

                    // [START long]
                    case 6:
                        flashSosTimingHandler.postDelayed(this, 1500);
                        break;
                    case 7:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    case 8:
                        flashSosTimingHandler.postDelayed(this, 1500);
                        break;
                    case 9:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    case 10:
                        flashSosTimingHandler.postDelayed(this, 1500);
                        break;
                    case 11:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    // [END long]

                    // [START short]
                    case 12:
                        flashSosTimingHandler.postDelayed(this, 500);
                        break;
                    case 13:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    case 14:
                        flashSosTimingHandler.postDelayed(this, 500);
                        break;
                    case 15:
                        flashSosTimingHandler.postDelayed(this, 300);
                        break;
                    case 16:
                        flashSosTimingHandler.postDelayed(this, 500);
                        break;
                    case 17:
                        // Final
                        flashSosTimingHandler.postDelayed(this, 3000);
                        flashSosTimer = 0;
                        break;
                    // [END short]
                }

                flashSosTimer++;

                if (isFlash) {
                    turnOffFlash("sos");
                } else {
                    turnOnFlash("");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get camera: " + e.getMessage());
            }
        }
    };
    // Switch Flash
    private Runnable switchRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (isFlash || isFlashSos) {
                    // turn off flash
                    turnOffFlash("");
                } else {
                    // turn on flash
                    turnOnFlash("");
                }
            } catch (RuntimeException e) {
                //Log.e(TAG, "SwitchFlash: " + e.getMessage());
            }
        }
    };
    // Switch FlashSos
    private Runnable switchSosRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (isFlashSos) {
                    // turn off flash
                    turnOffFlash("");
                    isFlashSos = false;
                } else if (!isFlash) {
                    // turn on flash
                    turnOnSos();
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "SwitchSos: " + e.getMessage());
            }
        }
    };

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction() != null ? intent.getAction() : "";

        switch (action) {
            case "switch":
            case "widget":
                new Thread(switchRunnable).start();
                break;
            case "switchSos":
                new Thread(switchSosRunnable).start();
                break;
            case "powerButton":
                //Log.e(TAG, "powerbutton");

                if (buttonPowercount == 0) {
                    powerButtonTask.run();
                }
                // 3 times
                if (buttonPowercount >= 2) {
                    new Thread(switchRunnable).start();
                    buttonPowercount = 0;
                    powerButtonTaskfirst = false;
                    powerButtonHandler.removeCallbacksAndMessages(null);
                } else {
                    buttonPowercount++;
                }


                break;
            case "app":
                updateUI.run();
                break;
        }

        return START_REDELIVER_INTENT;
        //return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy");

        turnOffFlash("");
        unregisterReceiver(mReceiver);
    }


}
