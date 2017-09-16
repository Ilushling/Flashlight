package ru.ilushling.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import static ru.ilushling.flashlight.R.id.buttonsos;
import static ru.ilushling.flashlight.R.id.buttonswitch;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String TAG = "MainActivity";

    ImageButton buttonSwitch, buttonSos;
    Camera camera;
    Camera.Parameters params;

    Drawable buttonSwitchOn, buttonSwitchOff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSwitch = (ImageButton) findViewById(buttonswitch);
        buttonSos = (ImageButton) findViewById(buttonsos);

        buttonSwitchOn = getResources().getDrawable(R.drawable.main_button_on);
        buttonSwitchOff = getResources().getDrawable(R.drawable.main_button_off);

        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
        // присвоим обработчик кнопке
        buttonSos.setOnClickListener(this);
        // присвоим обработчик кнопке
        buttonSwitch.setOnClickListener(this);

        // Service
        Intent intent = new Intent(MainActivity.this, MyReceiver.class);
        intent.setAction(MyReceiver.ACTION_APP);
        sendBroadcast(intent);

        //startService(new Intent(this, MyService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(broadcastReceiver, new IntentFilter(MyReceiver.ACTION_IS_FLASH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case buttonswitch:
                switchFlash();
                break;
            case buttonsos:
                switchFlashSos();
                break;
        }
    }

    void switchFlash() {
        Intent intent = new Intent(MainActivity.this, MyReceiver.class);
        intent.setAction(MyReceiver.ACTION_SWITCH);
        sendBroadcast(intent);
    }

    void switchFlashSos() {
        Intent intent = new Intent(MainActivity.this, MyReceiver.class);
        intent.setAction(MyReceiver.ACTION_SWITCH_SOS);
        sendBroadcast(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MyReceiver.ACTION_IS_FLASH:
                    // Get State of Flash
                    Boolean isFlash = intent.getBooleanExtra("isFlash", false);
                    // Update UI
                    if (isFlash) {
                        // Change Image
                        //Log.e(TAG, "Flash");
                        buttonSwitch.setImageDrawable(buttonSwitchOn);
                    } else {
                        // Change Image
                        buttonSwitch.setImageDrawable(buttonSwitchOff);
                    }
                    break;
            }
        }
    };
}
