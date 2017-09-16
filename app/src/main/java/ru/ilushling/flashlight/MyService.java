package ru.ilushling.flashlight;

import android.app.Notification;
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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class MyService extends Service {

    String TAG = "Service";

    BroadcastReceiver mReceiver;

    private Camera camera;
    private Parameters params;
    private CameraManager mCameraManager;
    private String mCameraId;
    private boolean isCameraGet = false, isFlash, isFlashSos;
    int buttonPowercount, flashSosTimer;
    boolean from_turnPowerFlash;
    boolean taskfirst;
    List<String> flashMode;

    @Override
    public void onCreate() {
        super.onCreate();

        // Screen listener
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new MyReceiver();
        registerReceiver(mReceiver, filter);

        // Notification Switch
        Intent intentSwitch = new Intent(this, MyReceiver.class);
        intentSwitch.setAction(MyReceiver.ACTION_SWITCH);
        PendingIntent pIntentSwitch = PendingIntent.getBroadcast(this, 0, intentSwitch, PendingIntent.FLAG_UPDATE_CURRENT);
        // Notification Close
        Intent intentServiceOff = new Intent(this, MyReceiver.class);
        intentServiceOff.setAction(MyReceiver.ACTION_SERVICE_OFF);
        PendingIntent pIntentServiceOff = PendingIntent.getBroadcast(this, 0, intentServiceOff, PendingIntent.FLAG_UPDATE_CURRENT);

        // Строим уведомление
        Notification builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            builder = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Фонарик")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pIntentSwitch)
                    .setContentText("Нажмите, чтобы переключить")
                    .addAction(R.mipmap.ic_launcher, "Выключить", pIntentServiceOff)
                    .build();
        }

        // убираем уведомление, когда его выбрали
        //builder.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, builder);
        startForeground(9955, builder);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String Action = intent.getAction();

        if (Action == "switch" || Action == "widget") {
            switchFlash();
        } else if (Action == "switchSos") {
            switchSos();
        } else {
            from_turnPowerFlash = intent.getBooleanExtra("power_button", false);
            if (from_turnPowerFlash) {
                //boolean screenOn = intent.getBooleanExtra("screen_state", false);
                if (buttonPowercount == 0) {
                    powerButtonTask.run();
                }
                if (buttonPowercount >= 2) {
                    switchFlash();
                    buttonPowercount = 0;
                } else {
                    buttonPowercount++;
                }
            }
        }

        return START_REDELIVER_INTENT;
        //return super.onStartCommand(intent, flags, startId);
    }

    // Get the camera
    void getCamera() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mCameraManager == null) {
                    mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    mCameraId = mCameraManager.getCameraIdList()[0];
                }
            } else {
                if (camera == null) {
                    // Open
                    camera = Camera.open();
                    params = camera.getParameters();
                    flashMode = params.getSupportedFlashModes();
                    params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(params);
                    camera.setPreviewTexture(new SurfaceTexture(1));
                    camera.startPreview();
                }
            }

            isCameraGet = true;
        } catch (Exception e) {
            isCameraGet = false;
            Log.e(TAG, "GetCamera: " + e.getMessage());
        }
    }

    void switchFlash() {
        try {
            if (isFlash || isFlashSos) {
                // turn off flash
                turnOffFlash(null);
            } else if (!isFlash) {
                // turn on flash
                turnOnFlash(null);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "SwitchFlash: " + e.getMessage());
        }
    }

    void switchSos() {
        try {
            if (isFlashSos) {
                // turn off flash
                turnOffFlash(null);
                isFlashSos = false;
            } else if (!isFlash && !isFlashSos) {
                // turn on flash
                turnOnSos();
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "SwitchSos: " + e.getMessage());
        }
    }

    void turnOnSos() {
        if (!isCameraGet) {
            getCamera();
        }

        isFlashSos = true;
        flashSosTimer = 0;
        flashSosTimerTask.run();
    }

    // TURN ON FLASH
    void turnOnFlash(String method) {
        try {
            //Log.e(TAG, "Service 1");
            isFlash = true;
            // UI
            if (method != "sos") {
                updateUI();
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
                        if (Build.VERSION.SDK_INT > 10) {
                            try {
                                camera.setPreviewTexture(new SurfaceTexture(0));
                            } catch (Exception e) {
                                Log.e("ale: ", "" + e);
                            }
                        }
                        camera.setParameters(params);
                        camera.startPreview();
                        //Log.e(TAG, "FLASH: " + flashMode);
                    } else {
                        if (flashMode.contains(Parameters.FLASH_MODE_ON)) {
                            params.setFlashMode(Parameters.FLASH_MODE_ON);
                        } else if (flashMode.contains(Parameters.FLASH_MODE_AUTO)) {
                            params.setFlashMode(Parameters.FLASH_MODE_AUTO);
                        } else if (flashMode.contains(Parameters.FLASH_MODE_RED_EYE)) {
                            params.setFlashMode(Parameters.FLASH_MODE_RED_EYE);
                        } else {
                            isFlash = false;
                            updateUI();
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to get camera: " + e.getMessage());
            isFlash = false;
        }
    }


    // TURN OFF FLASH
    void turnOffFlash(String method) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, false);
                if (method != "sos") {
                    flashSosTimingHandler.removeCallbacks(flashSosTimerTask);

                    mCameraManager = null;
                    mCameraId = null;

                    isCameraGet = false;
                    isFlash = false;
                    isFlashSos = false;
                }
            } else {
                if (camera != null) {
                    params.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                    if (method != "sos") {
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
                    isFlash = false;
                }
                isFlash = false;
            }

            // Update UI
            Intent intent = new Intent(MyService.this, MyReceiver.class);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.setAction(MyReceiver.ACTION_IS_FLASH);
            intent.putExtra("isFlash", isFlash);
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed turn off flash: " + e.getMessage());
        }
    }

    // power button zeroing counter
    private Handler powerButtonHandler = new Handler();
    private Runnable powerButtonTask = new Runnable() {
        @Override
        public void run() {
            if (!taskfirst) {
                powerButtonHandler.postDelayed(powerButtonTask, 1500);
                taskfirst = true;
            } else {
                buttonPowercount = 0;
                taskfirst = false;
            }

        }
    };

    // FlashSos timing
    private Handler flashSosTimingHandler = new Handler();
    private Runnable flashSosTimerTask = new Runnable() {
        @Override
        public void run() {

            try {
                // TIMING
                flashSosTimer++;
                // 18 because every 3 actions need 6 runs (3 on and 3 off)
                if (flashSosTimer >= 18) {
                    flashSosTimer = 0;
                    flashSosTimingHandler.postDelayed(this, 2000);
                } else {
                    if (flashSosTimer <= 6) {
                        flashSosTimingHandler.postDelayed(this, 350);
                    } else if (flashSosTimer <= 12) {
                        flashSosTimingHandler.postDelayed(this, 650);
                    } else {
                        flashSosTimingHandler.postDelayed(this, 350);
                    }
                    if (isFlash) {
                        turnOffFlash("sos");
                    } else {
                        turnOnFlash(null);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get camera: " + e.getMessage());
            }
        }
    };


    void updateUI() {
        Intent intent = new Intent(MyService.this, MyReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setAction(MyReceiver.ACTION_IS_FLASH);
        intent.putExtra("isFlash", isFlash);
        sendBroadcast(intent);
    }

    /*
    // Get camera async
    private class getCameraAsync extends AsyncTask<Void, Void, Camera> {
        @Override
        protected Camera doInBackground(Void... voids) {
            getCamera();
            return null;
        }
    };
    */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        turnOffFlash(null);
        unregisterReceiver(mReceiver);
        Log.e(TAG, "MyService onDestroy");
    }


}
