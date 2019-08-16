package ru.ilushling.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String TAG = "MainActivity";
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private AdView mAdView;
    ImageButton buttonSwitch, buttonSos;
    Bitmap buttonSwitchOn, buttonSwitchOff;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction() != null ? intent.getAction() : "") {
                case MyReceiver.ACTION_IS_FLASH:
                    // Get State of Flash
                    boolean isFlash = intent.getBooleanExtra("isFlash", false);
                    // Update UI
                    if (isFlash) {
                        // Change Image
                        //Log.e(TAG, "Flash");
                        buttonSwitch.setImageBitmap(buttonSwitchOn);
                    } else {
                        // Change Image
                        buttonSwitch.setImageBitmap(buttonSwitchOff);
                    }
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mAdView = findViewById(R.id.adView);
        MobileAds.initialize(this, getString(R.string.ad_app_id));

        Bundle bundle = new Bundle();
        mFirebaseAnalytics.logEvent("open_app", bundle);

        // ADS
        // Remote settings
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(false)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(remoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchRemoteConfig();
        boolean enableAd = mFirebaseRemoteConfig.getBoolean("enableAd");

        if (enableAd) {
            showAd();
        }
        Log.e(TAG, "enableAd: " + enableAd);

        buttonSwitch = findViewById(R.id.buttonSwitch);
        buttonSos = findViewById(R.id.buttonSos);

        Resources res = this.getResources();

        buttonSwitchOn = BitmapFactory.decodeResource(res, R.drawable.main_button_on);
        buttonSwitchOff = BitmapFactory.decodeResource(res, R.drawable.main_button_off);

        //Bitmap b = BitmapFactory.decodeResource(res, id);

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Service
        Intent intent = new Intent(MainActivity.this, MyReceiver.class);
        intent.setAction(MyReceiver.ACTION_APP);
        sendBroadcast(intent);

        registerReceiver(broadcastReceiver, new IntentFilter(MyReceiver.ACTION_IS_FLASH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Clean image memory
            buttonSwitchOn.recycle();
            buttonSwitchOff.recycle();
            // Free receiver
            unregisterReceiver(broadcastReceiver);
            // Kil process
            //int id = android.os.Process.myPid();
            //android.os.Process.killProcess(id);
        } catch (Exception e) {
            Log.e(TAG, "cant unregister receiver");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonSwitch:
                switchFlash();
                break;
            case R.id.buttonSos:
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

    void showAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(adRequest);
        mAdView.setVisibility(View.VISIBLE);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.e(TAG, "loaded");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.e(TAG, "failed: " + errorCode);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.

                Log.e(TAG, "opened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.

                Log.e(TAG, "left app");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.

                Log.e(TAG, "closed");
            }
        });
    }

    void hideAd() {
        mAdView.destroy();
        mAdView.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    void fetchRemoteConfig() {
        // cache expiration in seconds
        int cacheExpiration = 30;
        //expire the cache immediately for development mode.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        }
                    }
                });
    }
}
