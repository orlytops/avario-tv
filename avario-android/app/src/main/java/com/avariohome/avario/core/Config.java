package com.avariohome.avario.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.avariohome.avario.R;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.PlatformUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by aeroheart-c6 on 12/01/2017.
 */
public class Config {
    public static final String TAG = "Avario/Config";

    private static final String PREFKEY_RES_FETCHED = "setting__res_fetched";
    private static final String PREFKEY_HTTP_HOST = "setting__http_host";
    private static final String PREFKEY_HTTP_PORT = "setting__http_port";
    private static final String PREFKEY_HTTP_SSL = "setting__http_ssl";
    private static final String PREFKEY_USERNAME = "setting__username";
    private static final String PREFKEY_PASSWORD = "setting__password";
    private static final String PREFKEY_ASSET_ROOT = "setting__assets";
    private static final String PREFKEY_BOOTSTRAP = "setting__bootstrap";
    private static final String PREFKEY_HOLD_SECONDS = "setting__hold_seconds";
    private static final String PREFKEY_LIGHT_ALGO = "algo_light";

    private static Config instance = null;

    private Context context;
    private StateArray state;
    private SharedPreferences prefs;

    public static Config getInstance() {
        return Config.instance;
    }

    /**
     * Retrieves the singleton instance of this class. It is important to pass the application
     * context here because this object will be alive throughout the duration of the app.
     *
     * @param context the application context
     * @return instance of the APIClient singleton
     */
    public static Config getInstance(Context context) {
        if (Config.instance == null)
            Config.instance = new Config(context);

        return Config.instance;
    }

    private Config(Context context) {
        super();

        this.context = context;
        this.state = StateArray.getInstance();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isSet() {
        String httpHost = this.getHttpHost(),
                username = this.getUsername(),
                password = this.getPassword(),
                httpPort = this.getHttpPort();

        return (httpHost != null && httpHost.length() > 0)
                && (httpPort != null && httpPort.length() > 0)
                && (username != null && username.length() > 0)
                && (password != null && password.length() > 0);
    }

    public boolean isResourcesFetched() {
        return this.fetchBoolean(PREFKEY_RES_FETCHED);
    }

    public String getHttpDomain() {
        return String.format("http%s://%s:%s",
                this.isHttpSSL() ? "s" : "",
                this.getHttpHost(),
                this.getHttpPort()
        );
    }

    public String getHttpHost() {
        return this.fetchString(PREFKEY_HTTP_HOST);
    }

    public String getHttpPort() {
        return this.fetchString(PREFKEY_HTTP_PORT);
    }

    public boolean isHttpSSL() {
        return this.fetchBoolean(PREFKEY_HTTP_SSL);
    }

    public String getUsername() {
        return this.fetchString(PREFKEY_USERNAME);
    }

    public String getPassword() {
        return this.fetchString(PREFKEY_PASSWORD);
    }

    public String getAssetRoot() {
        return String.format("%s/x%%.1f", this.fetchString(
                PREFKEY_ASSET_ROOT,
                R.string.app__url__assetroot)
        );
    }

    public String getBootstrapURL() {
        return String.format("%s%s",
                this.getHttpDomain(),
                this.fetchString(PREFKEY_BOOTSTRAP, R.string.app__url__bootstrap)
        );
    }

    public int getSettingsHoldDelay() {
        int seconds;

        try {
            seconds = this.state.getSettingsHoldDelay();
        } catch (AvarioException exception) {
            PlatformUtil.logError(exception);
            seconds = this.fetchInteger(PREFKEY_HOLD_SECONDS, R.integer.app__setting__holdmsec);
        }

        return seconds;
    }

    public int getAPIErrorDelay() {
        int seconds;

        try {
            seconds = this.state.getAPIErrorDelay();
        } catch (AvarioException exception) {
            seconds = this.context
                    .getResources()
                    .getInteger(R.integer.timer__apierror);
        }

        return seconds;
    }

    public int getNagleDelay() {
        int seconds;

        try {
            seconds = this.state.getNagleDelay();
        } catch (AvarioException exception) {
            seconds = this.context
                    .getResources()
                    .getInteger(R.integer.timer__nagle);
        }

        return seconds;
    }

    public int getNagleMediaDelay() {
        int seconds;

        try {
            seconds = this.state.getNagleMediaDelay();
        } catch (AvarioException exception) {
            seconds = this.context
                    .getResources()
                    .getInteger(R.integer.timer__nagle);
        }

        return seconds;
    }

    public int getInactivityDelay() {
        int seconds;

        try {
            seconds = this.state.getInactivityDelay();
        } catch (AvarioException exception) {
            seconds = this.context
                    .getResources()
                    .getInteger(R.integer.timer__inactivity);
        }

        return seconds;
    }

    public int getPostBLEDelay() {
        int seconds;

        try {
            seconds = this.state.getPostBLEDelay();
        } catch (AvarioException exception) {
            seconds = this.context
                    .getResources()
                    .getInteger(R.integer.timer_post_ble);
        }

        return seconds;
    }

    public void setResourcesFetched(boolean fetched) {
        this.prefs.edit().putBoolean(PREFKEY_RES_FETCHED, fetched).commit();
    }


    public void setHttpHost(String httpHost) {
        this.prefs.edit().putString(PREFKEY_HTTP_HOST, httpHost).commit();
    }

    public void setHttpPort(String httpPort) {
        this.prefs.edit().putString(PREFKEY_HTTP_PORT, httpPort).commit();
    }

    public void setHttpSSL(boolean ssl) {
        this.prefs.edit().putBoolean(PREFKEY_HTTP_SSL, ssl).commit();
    }

    public void setUsername(String username) {
        this.prefs.edit().putString(PREFKEY_USERNAME, username).commit();
    }

    public void setPassword(String password) {
        this.prefs.edit().putString(PREFKEY_PASSWORD, password).commit();
    }

    public void clear() {
        this.prefs.edit()
                .clear()
                .commit();
    }

    private boolean fetchBoolean(String key) {
        return this.prefs.getBoolean(key, false);
    }

    private int fetchInteger(String key, int referenceId) {
        int response = Integer.parseInt(this.prefs.getString(key, "-1"));

        if (response == -1)
            response = this.context.getResources().getInteger(referenceId);

        return response;
    }

    private String fetchString(String key, int referenceId) {
        String response = this.prefs.getString(key, null);

        if (response == null)
            response = this.context.getString(referenceId);

        return response;
    }

    private String fetchString(String key) {
        return this.prefs.getString(key, null);
    }

    public void setLightAlgo(ArrayList<Light.Algo> algos) {
        String set = new Gson().toJson(algos);
        this.prefs.edit().putString(PREFKEY_LIGHT_ALGO, set).apply();
    }

    public ArrayList<Light.Algo> getLightAlgo() {
        return new Gson().fromJson(this.prefs.getString(PREFKEY_LIGHT_ALGO, null),
                new TypeToken<ArrayList<Light.Algo>>() {
                }.getType());
    }
}
