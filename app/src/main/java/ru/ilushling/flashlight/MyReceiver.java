package ru.ilushling.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class MyReceiver extends BroadcastReceiver {

    String TAG = "MyReceiver";
    private boolean screenOff, isFlash;

    public static final String ACTION_IS_FLASH = "ru.ilushling.flashlight.isflash",
            ACTION_APP = "ru.ilushling.flashlight.app",
            ACTION_SERVICE_OFF = "ru.ilushling.flashlight.serviceoff",
            ACTION_WIDGET = "ru.ilushling.flashlight.widget",
            ACTION_SWITCH = "ru.ilushling.flashlight.switch",
            ACTION_SWITCH_SOS = "ru.ilushling.flashlight.switchSos";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_IS_FLASH)) {
            // Flash
            isFlash = intent.getBooleanExtra("isFlash", false);
            // Widget
            updateWidget(context, isFlash);
            // Activity
            Intent it = new Intent();
            it.setAction(MyReceiver.ACTION_IS_FLASH);
            it.putExtra("isFlash", isFlash);
            context.sendBroadcast(it);
        }
        if (intent.getAction().equals(ACTION_APP)) {
            Intent i = new Intent(context, MyService.class);
            i.setAction("app");
            context.startService(i);
        }
        if (intent.getAction().equals(ACTION_WIDGET)) {
            Intent i = new Intent(context, MyService.class);
            i.setAction("widget");
            context.startService(i);
        }
        if (intent.getAction().equals(ACTION_SWITCH)) {
            Intent i = new Intent(context, MyService.class);
            i.setAction("switch");
            context.startService(i);
        }
        if (intent.getAction().equals(ACTION_SWITCH_SOS)) {
            Intent i = new Intent(context, MyService.class);
            i.setAction("switchSos");
            context.startService(i);
        }
        if (intent.getAction().equals(ACTION_SERVICE_OFF)) {
            Intent i = new Intent(context, MyService.class);
            i.setAction("serviceOff");
            context.stopService(i);
        }

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOff = true;

            Intent i = new Intent(context, MyService.class);
            i.putExtra("screen_state", screenOff);
            i.putExtra("power_button", true);
            context.startService(i);
            //Log.e("daun: ", "t");
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOff = false;

            Intent i = new Intent(context, MyService.class);
            i.putExtra("screen_state", screenOff);
            i.putExtra("power_button", true);
            context.startService(i);
            //Log.e("daun: ", "t1");
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
