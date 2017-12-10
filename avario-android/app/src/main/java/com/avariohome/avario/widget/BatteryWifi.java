package com.avariohome.avario.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
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

        registerReceiver();
    }

    private void setWifiLevel(int level) {
        int assetId = 0;
        switch (level) {
            case 1:
                if (isLan) {
                    assetId = R.array.ic__wifi__green__1;
                } else {
                    assetId = R.array.ic__wifi__red__1;
                }
                break;
            case 2:
                if (isLan) {
                    assetId = R.array.ic__wifi__green__2;
                } else {
                    assetId = R.array.ic__wifi__red__2;
                }
                break;
            case 3:
                if (isLan) {
                    assetId = R.array.ic__wifi__green__3;
                } else {
                    assetId = R.array.ic__wifi__red__3;
                }
                break;
            case 4:
                if (isLan) {
                    assetId = R.array.ic__wifi__green__4;
                } else {
                    assetId = R.array.ic__wifi__red__4;
                }
                break;
            case 5:
                if (isLan) {
                    assetId = R.array.ic__wifi__green__4;
                } else {
                    assetId = R.array.ic__wifi__red__4;
                }
                break;
        }

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
            wifiImage.setVisibility(GONE);
            percentText.setText(level + "%");
            percentText.setVisibility(VISIBLE);
            arc.setProgressColor(getResources().getColor(R.color.red));
        } else if (level >= 6 && level <= 10) {
            wifiImage.setVisibility(VISIBLE);
            percentText.setVisibility(GONE);
            arc.setProgressColor(getResources().getColor(R.color.orange));
        } else {
            wifiImage.setVisibility(VISIBLE);
            percentText.setVisibility(GONE);
            arc.setProgressColor(getResources().getColor(R.color.green));
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
