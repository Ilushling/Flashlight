package ru.ilushling.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class autoBoot extends BroadcastReceiver {
    public autoBoot() {
    }

    public void onReceive(Context context, Intent intent) {
        //context.startService(new Intent(context, FlashlightService.class));
    }
}
