package ru.ilushling.flashlight;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class Widgetprovider extends AppWidgetProvider {


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            // initializing widget layout
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widgetlayout);

            // register for button event
            remoteViews.setOnClickPendingIntent(R.id.actionButton, buildButtonPendingIntent(context, MyReceiver.ACTION_WIDGET));

            //Log.e("Error", "update widget button1");
            // request for widget update
            pushWidgetUpdate(context, remoteViews, false);
        } catch (NullPointerException e) {
            Log.e("Error", "fail camera1: " + e.getMessage());
        }
    }

    public void onReceive(Context context, Intent intent) {
        //Log.e("Error", "update widget button2");
        if (intent.getAction() == MyReceiver.ACTION_WIDGET) {
            //Log.e("Error", "update widget button3");
            // Service
            intent = new Intent(context, MyReceiver.class);
            intent.setAction(MyReceiver.ACTION_WIDGET);
            context.sendBroadcast(intent);
        }
        super.onReceive(context, intent);//add this line
    }

    public static PendingIntent buildButtonPendingIntent(Context context, String action) {

        // initiate widget update request
        Intent intent = new Intent(context, Widgetprovider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public static void pushWidgetUpdate(Context context, RemoteViews remoteViews, boolean isFlashOn) {
        try {
            //Log.e("Error", "update widget: " + isFlashOn);
            ComponentName myWidget = new ComponentName(context, Widgetprovider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);

            // updating view with initial data
            if (isFlashOn) {
                remoteViews.setImageViewResource(R.id.actionButton, R.drawable.widget_flash_on);
            } else {
                remoteViews.setImageViewResource(R.id.actionButton, R.drawable.widget_flash_off);
            }

            // register for button event
            remoteViews.setOnClickPendingIntent(R.id.actionButton, buildButtonPendingIntent(context, MyReceiver.ACTION_WIDGET));
            manager.updateAppWidget(myWidget, remoteViews);
        } catch (NullPointerException e) {
            Log.e("Error", "fail camera3: " + e.getMessage());
        }
    }

}