package ru.ilushling.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;


public class MyReceiver extends BroadcastReceiver {

    String TAG = "MyReceiver";
    private boolean screenState, isFlash;

    public static final String ACTION_IS_FLASH = "ru.ilushling.flashlight.isflash",
            ACTION_APP = "ru.ilushling.flashlight.app",
            ACTION_SERVICE_OFF = "ru.ilushling.flashlight.serviceoff",
            ACTION_WIDGET = "ru.ilushling.flashlight.widget",
            ACTION_SWITCH = "ru.ilushling.flashlight.switch",
            ACTION_SWITCH_SOS = "ru.ilushling.flashlight.switchSos";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction() != null ? intent.getAction() : "";

        if (action.equals(ACTION_IS_FLASH)) {
            // Flash
            isFlash = intent.getBooleanExtra("isFlash", false);
            // Activity
            Intent it = new Intent();
            it.setAction(MyReceiver.ACTION_IS_FLASH);
            it.putExtra("isFlash", isFlash);
            context.sendBroadcast(it);
            // Widget
            updateWidget(context, isFlash);
        }
        if (action.equals(ACTION_APP)) {
            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("app");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        }
        if (action.equals(ACTION_WIDGET)) {
            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("widget");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        }
        if (action.equals(ACTION_SWITCH)) {
            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("switch");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        }
        if (action.equals(ACTION_SWITCH_SOS)) {
            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("switchSos");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        }
        if (action.equals(ACTION_SERVICE_OFF)) {
            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("serviceOff");
            context.stopService(i);
        }

        if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            screenState = false;

            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("powerButton");
            i.putExtra("screen_state", screenState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
            //Log.e(TAG, "screenStateOff = " + screenState);
        } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            screenState = true;

            Intent i = new Intent(context, FlashlightService.class);
            i.setAction("powerButton");
            i.putExtra("power_button", screenState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
            //Log.e(TAG, "screenStateOn = " + screenState);
        }


    }

    public void updateWidget(Context context, boolean isFlashOn) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);

            views.setImageViewResource(R.id.actionButton, R.drawable.widget_flash_on);
            views.setOnClickPendingIntent(R.id.actionButton,
                    Widgetprovider.buildButtonPendingIntent(context, MyReceiver.ACTION_WIDGET));
            Widgetprovider.pushWidgetUpdate(context.getApplicationContext(),
                    views, isFlashOn);

        } catch (Exception exc) {
            Log.e("daun2: ", "" + exc);
        }
    }
}
