package com.avariohome.avario.widget;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.avariohome.avario.R;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.Connectivity;
import com.triggertrap.seekarc.SeekArc;

/**
 * Created by orly on 10/30/17.
 */

public class BatteryWifi extends FrameLayout {

    private SeekArc arc;
    private ImageView wifiImage;
    private TextView percentText;

    private boolean isLan = false;
    private int levelWifi = 0;


    private BatteryBroadcastReceiver mReceiver;
    private WifiBroadcastReceiver wifiBroadcastReceiver;


    private AlertDialog.Builder builder;
    private AlertDialog alert11;


    private boolean animationStart = false;
    private boolean isAnimate = false;

    public BatteryWifi(@NonNull Context context) {
        super(context);
        init();
    }

    public BatteryWifi(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryWifi(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        isLan = Connectivity.isConnectedToLan();
        LayoutInflater inflater;

        inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(R.layout.battery_wifi, this);

        arc = (SeekArc) this.findViewById(R.id.arc);
        wifiImage = (ImageView) findViewById(R.id.image_wifi);
        percentText = (TextView) findViewById(R.id.text_percent);

        mReceiver = new BatteryBroadcastReceiver();
        wifiBroadcastReceiver = new WifiBroadcastReceiver();

        arc.setEnabled(false);
        arc.setMax(100);

        builder = new AlertDialog.Builder(getContext());
        registerReceiver();
    }

    private void setWifiLevel(int level) {
        int assetId = 0;
        switch (level) {
            case 1:
                if (isLan) {
                    assetId = R.array.ic__wifi__blue__1;
                } else {
                    assetId = R.array.ic__wifi__red__1;
                }
                break;
            case 2:
                if (isLan) {
                    assetId = R.array.ic__wifi__blue__2;
                } else {
                    assetId = R.array.ic__wifi__red__2;
                }
                break;
            case 3:
                if (isLan) {
                    assetId = R.array.ic__wifi__blue__3;
                } else {
                    assetId = R.array.ic__wifi__red__3;
                }
                break;
            case 4:
                if (isLan) {
                    assetId = R.array.ic__wifi__blue__4;
                } else {
                    assetId = R.array.ic__wifi__red__4;
                }
                break;
            case 5:
                if (isLan) {
                    assetId = R.array.ic__wifi__blue__4;
                } else {
                    assetId = R.array.ic__wifi__red__4;
                }
                break;
        }


        /*if (assetId != 0) {
            JSONArray maclist = null;
            try {
                maclist = StateArray.getInstance().getLanMacList();
            } catch (AvarioException e) {
                e.printStackTrace();
            }

            String message = "Bootstrap Mac list: " + maclist.toString() + "\n" + "Connection MAC: " +
                    Connectivity.getAccessPointMac(getContext()) + "\n" + "Asset Id: " + getResources().getResourceEntryName(assetId);
            builder.setTitle("MAC info")
                    .setMessage(message)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    });
            if (alert11 == null) {
                alert11 = builder.create();
            }

            if (!alert11.isShowing()) {
                alert11.show();
            }

        }*/

        if (assetId == 0) {
            wifiImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_close));
            return;
        }
        AssetUtil.toDrawable(
                getContext(),
                assetId,
                new AssetUtil.ImageViewCallback(wifiImage)
        );
    }

    public void setIsLan(boolean isLan) {
        this.isLan = isLan;
        setWifiLevel(levelWifi);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mReceiver);
        getContext().unregisterReceiver(wifiBroadcastReceiver);
    }

    private void setBatteryLevel(int level) {
        arc.setProgress(level);
        if (level >= 0 && level <= 5) {
            isAnimate = true;
            wifiImage.setVisibility(GONE);
            percentText.setText(level + "%");
            percentText.setVisibility(VISIBLE);
            if (!animationStart) {
                fadeIn(percentText);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        wifiImage.setVisibility(VISIBLE);
                        fadeIn(wifiImage);
                    }
                }, 4500);
            }
            arc.setProgressColor(getResources().getColor(R.color.red));
        } else if (level >= 6 && level <= 10) {
            isAnimate = false;
            animationStart = false;
            wifiImage.setVisibility(VISIBLE);
            percentText.setVisibility(GONE);
            arc.setProgressColor(getResources().getColor(R.color.orange));
        } else {
            isAnimate = false;
            animationStart = false;
            wifiImage.setVisibility(VISIBLE);
            percentText.setVisibility(GONE);
            arc.setProgressColor(getResources().getColor(R.color.blue));
        }
    }

    public void fadeIn(final View view) {
        final Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(500);
        fadeIn.setFillAfter(true);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fadeOut(view);
                    }
                }, 3500);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        if (isAnimate) {
            view.startAnimation(fadeIn);
            animationStart = true;
        } else {
            animationStart = false;
        }
    }

    public void fadeOut(final View view) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fadeIn(view);
                    }
                }, 3500);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        if (isAnimate) {
            view.startAnimation(fadeOut);
        } else {
            animationStart = false;
        }
    }


    public int getWifiSignalStrength(Context context) {
        int MIN_RSSI = -100;
        int MAX_RSSI = -55;
        int levels = 5;
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        int rssi = info.getRssi();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return WifiManager.calculateSignalLevel(info.getRssi(), levels);
        } else {
            // this is the code since 4.0.1
            if (rssi <= MIN_RSSI) {
                return 0;
            } else if (rssi >= MAX_RSSI) {
                return levels - 1;
            } else {
                float inputRange = (MAX_RSSI - MIN_RSSI);
                float outputRange = (levels - 1);
                return (int) ((float) (rssi - MIN_RSSI) * outputRange / inputRange);
            }
        }
    }


    private void registerReceiver() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        try {
            getContext().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {

        }

        try {
            getContext().registerReceiver(wifiBroadcastReceiver, mIntentFilter);
        } catch (Exception e) {

        }
    }

    private class BatteryBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            setBatteryLevel(level);
        }
    }

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Signal Level", getWifiSignalStrength(getContext()) + "");
            setIsLan(Connectivity.isConnectedToLan());
            levelWifi = getWifiSignalStrength(getContext());
            setWifiLevel(levelWifi);
        }
    }

}
