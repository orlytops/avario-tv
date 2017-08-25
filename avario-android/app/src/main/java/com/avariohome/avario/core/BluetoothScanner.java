package com.avariohome.avario.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.avariohome.avario.Application;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIRequestListener;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.PlatformUtil;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneEID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneUID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;

/**
 * Created by memengski on 7/7/17.
 */
public class BluetoothScanner {
  public static final String TAG = "Avario/BluetoothScanner";
  public static final String TIMER_ID = "bluetooth-scanner-id";

  // Eddystone service uuid (0xfeaa)
  private static final ParcelUuid UID_SERVICE =
      ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb");

  private static final BluetoothScanner instance = new BluetoothScanner();

  private Context          context;
  private BluetoothAdapter bluetoothAdapter;

  private JSONArray devices;

  private Handler  handler;
  private Runnable delayRunnable;

  public static BluetoothScanner getInstance() {
    return BluetoothScanner.instance;
  }

  public void setContext(Context context) {
    this.context = context;
    init();
  }

  private void init() {
    final BluetoothManager bluetoothManager =
        (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

    this.bluetoothAdapter = bluetoothManager.getAdapter();
    this.devices = new JSONArray();

    this.handler = Application.workHandler;
    this.delayRunnable = new DelayRunnable();
  }

  public BluetoothAdapter getAdapter() {
    return bluetoothAdapter;
  }

  public boolean isEnabled(){
    return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
  }

  public void scanLeDevice(final boolean enable) {
    if (!isEnabled())
      return;

    if (enable) {
//      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//      ScanFilter beaconFilter = new ScanFilter.Builder()
//          .setServiceUuid(UID_SERVICE)
//          .build();
//
//      List<ScanFilter> filters = new ArrayList<>();
//      filters.add(beaconFilter);
//
//      ScanSettings settings = new ScanSettings.Builder()
//          .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//          .setReportDelay(0)
//          .build();

      bluetoothAdapter.startLeScan(callback);
      startDelayTimer();
    } else {
      this.handler.removeCallbacks(this.delayRunnable);
      bluetoothAdapter.stopLeScan(callback);
    }
  }

  private void startDelayTimer() {
    if (!isEnabled())
      return;
    
    Config config = Config.getInstance();

    this.handler.removeCallbacks(this.delayRunnable);
    this.handler.postDelayed(
        this.delayRunnable,
        config.getPostBLEDelay()
    );
  }

  private JSONObject createJsonObject(final String address, final int rssi) {
    JSONObject jsonObject = new JSONObject();

    try {
      if (!TextUtils.isEmpty(address))
        jsonObject.put("mac", address);
    } catch (JSONException exception){}

    try {
      jsonObject.put("rssi", rssi);
    } catch (JSONException exception){}

    return jsonObject;
  }

  private void addToPayloadJSON(JSONObject jsonObject) {
    boolean isExisted = false;
    for (int x = 0; x < devices.length(); x++) {
      try {
        JSONObject object = devices.getJSONObject(x);
        if (object.getString("mac").equals(jsonObject.getString("mac"))) {
          devices.remove(x);
          devices.put(jsonObject);
          isExisted = true;
        }
      } catch (JSONException exception) {}
    }

    if (!isExisted)
      devices.put(jsonObject);
  }

  private BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
      List<ADStructure> structures = ADPayloadParser.getInstance().parse(scanRecord);
      JSONObject jsonObject = createJsonObject(device.getAddress(), rssi);

      for (ADStructure structure : structures) {
        if (structure instanceof IBeacon) {
          IBeacon iBeacon = (IBeacon) structure;

          try {
            if(!TextUtils.isEmpty(iBeacon.getUUID().toString()))
              jsonObject.put("uuid", iBeacon.getUUID().toString());
          } catch (JSONException exception){}

          try {
            jsonObject.put("major", iBeacon.getMajor());
          } catch (JSONException exception){}

          try {
            jsonObject.put("minor", iBeacon.getMinor());
          } catch (JSONException exception){}

          try {
            jsonObject.put("tx_power", iBeacon.getPower());
          } catch (JSONException exception){}

          addToPayloadJSON(jsonObject);
          return;
        } else if (structure instanceof EddystoneUID) {
          EddystoneUID eddystoneUID = (EddystoneUID) structure;

          try {
            jsonObject.put("tx_power", eddystoneUID.getTxPower());
          } catch (JSONException exception){}

          final String namespaceIdAsString = eddystoneUID.getNamespaceIdAsString();
          try {
            if(!TextUtils.isEmpty(namespaceIdAsString))
              jsonObject.put("name_space_id", namespaceIdAsString);
          } catch (JSONException exception){}

          final String instanceId = eddystoneUID.getInstanceIdAsString();
          try {
            if(!TextUtils.isEmpty(instanceId))
              jsonObject.put("instanceId", instanceId);
          } catch (JSONException exception){}

          final String beaconId = eddystoneUID.getBeaconIdAsString();
          try {
            if(!TextUtils.isEmpty(beaconId))
              jsonObject.put("beaconId", beaconId);
          } catch (JSONException exception){}

          addToPayloadJSON(jsonObject);
          return;
        } else if (structure instanceof EddystoneURL) {
          EddystoneURL eddystoneURL = (EddystoneURL) structure;

          try {
            jsonObject.put("tx_power", eddystoneURL.getTxPower());
          } catch (JSONException exception){}

          final URL url = eddystoneURL.getURL();
          try {
            if(url != null && !TextUtils.isEmpty(url.getPath()))
            jsonObject.put("url", url.toString());
          } catch (JSONException exception){}

          addToPayloadJSON(jsonObject);
          return;
        } else if (structure instanceof EddystoneTLM) {
          EddystoneTLM eddystoneTLM = (EddystoneTLM)structure;

          try {
            jsonObject.put("tlm_version", eddystoneTLM.getTLMVersion());
          } catch (JSONException exception){}

          try {
            jsonObject.put("battery_voltage", eddystoneTLM.getBatteryVoltage());
          } catch (JSONException exception){}

          try {
            jsonObject.put("beacon_temperature", eddystoneTLM.getBeaconTemperature());
          } catch (JSONException exception){}

          try {
            jsonObject.put("advertisement_count", eddystoneTLM.getAdvertisementCount());
          } catch (JSONException exception){}

          try {
            jsonObject.put("elapsed_time", eddystoneTLM.getElapsedTime());
          } catch (JSONException exception){}

          addToPayloadJSON(jsonObject);
          return;
        } else if (structure instanceof EddystoneEID) {
          EddystoneEID eddystoneEID = (EddystoneEID) structure;

          try {
            jsonObject.put("tx_power", eddystoneEID.getTxPower());
          } catch (JSONException exception){}

          final String eid = eddystoneEID.getEIDAsString();
          try {
            if(!TextUtils.isEmpty(eid))
              jsonObject.put("eid", eid);
          } catch (JSONException exception){}

          addToPayloadJSON(jsonObject);
          return;
        }
      }
    }
  };

  private class APIListener extends APIRequestListener<String> {

    APIListener() {
      super(TIMER_ID, new String[]{TIMER_ID});
    }

    @Override
    public void onDone(String response, VolleyError error) {
      super.onDone(response, error);
      startDelayTimer();
    }

    @Override
    protected void startTimer() {}

    @Override
    protected void forceTimerExpire() {}
  }

  private class DelayRunnable implements Runnable {
    @Override
    public void run() {
      JSONObject payloadJSON = new JSONObject();

      try {
        payloadJSON.put("tablet_id", PlatformUtil.getTabletId());
      } catch (JSONException exception){}

      try {
        payloadJSON.put("beaconScan", devices);
      } catch (JSONException exception){}

      StateArray states = StateArray.getInstance();
      JSONObject requestJSON;

      try {
         requestJSON = states.getBluetoothEndpointRequest();
      } catch (AvarioException exception){
        startDelayTimer();
        return;
      }

      try {
        if(devices.length() == 0) {
          startDelayTimer();
          return;
        }

        requestJSON.put("payload", payloadJSON.toString());
      } catch (JSONException exception){}

      try {
        APIClient.getInstance().executeRequest(requestJSON, "", TIMER_ID, new APIListener());
      } catch (AvarioException exception){
        startDelayTimer();
      }
    }
  }
}
