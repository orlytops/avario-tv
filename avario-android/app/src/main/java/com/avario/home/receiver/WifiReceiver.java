package com.avario.home.receiver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import com.avario.home.bus.WifiChange;
import com.avario.home.bus.WifiConnected;
import com.avario.home.util.Connectivity;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by orly on 10/10/17.
 */


public class WifiReceiver extends BroadcastReceiver {

    public int TYPE_WIFI = 1;
    public int TYPE_MOBILE = 2;
    public int TYPE_NOT_CONNECTED = 0;

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("WifiReceiver", action);
        String statusString = getConnectivityStatusString(context);
        if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
            Log.d("WifiReceiver", ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
            SupplicantState supl_state = ((SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
            EventBus.getDefault().post(new WifiConnected(true));
            switch (supl_state) {
                case ASSOCIATED:
                    Log.i("SupplicantState", "ASSOCIATED");
                    break;
                case ASSOCIATING:
                    Log.i("SupplicantState", "ASSOCIATING");
                    break;
                case AUTHENTICATING:
                    Log.i("SupplicantState", "Authenticating...");
                    break;
                case COMPLETED:
                    Log.i("SupplicantState", "Connected");
                    break;
                case DISCONNECTED:
                    Log.i("SupplicantState", "Disconnected");
                    break;
                case DORMANT:
                    Log.i("SupplicantState", "DORMANT");
                    break;
                case FOUR_WAY_HANDSHAKE:
                    Log.i("SupplicantState", "FOUR_WAY_HANDSHAKE");
                    break;
                case GROUP_HANDSHAKE:
                    Log.i("SupplicantState", "GROUP_HANDSHAKE");
                    break;
                case INACTIVE:
                    Log.i("SupplicantState", "INACTIVE");
                    break;
                case INTERFACE_DISABLED:
                    Log.i("SupplicantState", "INTERFACE_DISABLED");
                    break;
                case INVALID:
                    Log.i("SupplicantState", "INVALID");
                    break;
                case SCANNING:
                    Log.i("SupplicantState", "SCANNING");
                    break;
                case UNINITIALIZED:
                    Log.i("SupplicantState", "UNINITIALIZED");
                    break;
                default:
                    Log.i("SupplicantState", "Unknown");
                    break;

            }
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI){
                Log.d("WifiReceiver", "Have Wifi Connection");
                Connectivity.getInstance().myMacAddress = Connectivity.getMacAddress(context);
            } else
                Log.d("WifiReceiver", "Don't have Wifi Connection");

            int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
            if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                EventBus.getDefault().post(new WifiChange(true));
                Log.i("ERROR_AUTHENTICATING", "ERROR_AUTHENTICATING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Authentication Error");
                builder.setMessage("Unable to connect to WIFI. Please check your wifi settings.");
                builder.setCancelable(false);

                builder.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                            }
                        });

                AlertDialog alert11 = builder.create();
                alert11.show();
                context.unregisterReceiver(WifiReceiver.this);
            }

        }
    }

    public int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public String getConnectivityStatusString(Context context) {
        int conn = getConnectivityStatus(context);
        String status = null;
        if (conn == TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (conn == TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }

}
