package com.avariohome.avario.util;


import android.content.Context;
import android.text.TextUtils;

import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIMultiListener;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.NagleTimers;
import com.avariohome.avario.exception.AvarioException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by aeroheart-c6 on 02/03/2017.
 */
public class EntityUtil {
    /**
     * Reads from the icon_on or icon_off depending on the entity's state. The method will always
     * fall back to icon_off.
     *
     * @param entityJSON
     * @return url value from icon_on / icon_off attributes contained in an array for easy
     * integration with AssetUtil loaders
     * @throws AvarioException when icon_off property cannot be found from within the entity
     */
    public static String[] getStateIconUrl(Context context, JSONObject entityJSON) throws AvarioException {
        String property;

        try {
            property = "icon_" + entityJSON
                    .getJSONObject("new_state")
                    .getString("state");
        } catch (JSONException exception) {
            property = "icon_off";
        }

        if (!entityJSON.has(property))
            property = "icon_off";

        try {
            return AssetUtil.toAbsoluteURLs(
                    context,
                    new String[]{entityJSON.getString(property)}
            );
        } catch (JSONException exception) {
            return new String[]{""};
        }
    }

    public static int getNagleDelay(JSONObject entityJSON) {
        return entityJSON.optInt("nagle", Config.getInstance().getNagleDelay());
    }

    public static int getMediaNagleDelay(JSONObject entityJSON) {
        return entityJSON.optInt("nagle", Config.getInstance().getNagleMediaDelay());
    }

    public static int getSeekPosition(JSONObject mediaJSON) throws AvarioException {
        String entityId = mediaJSON.optString("entity_id");

        try {
            return (int) mediaJSON.getDouble("media_position_live");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("%s.new_state.state", entityId)
                    }
            );
        }
    }

    /**
     * Gets the `controls.gui.clk|dbl|lng` JSON array data from the entity. This is mainly used for
     * entities under the `entities` root object and nothing else. Should an incompatible entity
     * JSON be passed, it will simply return null
     *
     * @param entityJSON the entity
     * @param type       the "clk", "dbl", "lng" strings depending on the tap type it received
     * @return the JSONArray describing the GUI commands in the form of ["A", "H", "D", "L"]
     */
    public static JSONArray getGUICommands(JSONObject entityJSON, String type) {
        try {
            return entityJSON
                    .getJSONObject("controls")
                    .getJSONObject("gui")
                    .getJSONArray(type);
        } catch (JSONException | NullPointerException exception) {
            return null;
        }
    }

    /**
     * Gets the `controls.api.clk|dbl|lng` JSON array data from the entity. This is mainly used for
     * entities under the `entities` root object and nothing else. Should an incompatible entity
     * JSON be passed, it will simply return null
     *
     * @param entityJSON the entity
     * @param type       the "clk", "dbl", "lng" strings depending on the tap type it received
     * @return the JSONArray describe the API commands to perform
     */
    public static JSONArray getAPICommands(JSONObject entityJSON, String type) {
        try {
            return entityJSON
                    .getJSONObject("controls")
                    .getJSONObject("api")
                    .getJSONArray(type);
        } catch (JSONException | NullPointerException exception) {
            return null;
        }
    }

    /**
     * Runs the `controls.api.clk|dbl|lng` JSON array data from the entity. This is mainly used for
     * entities under the `entities` root object and nothing else. Should an incompatible entity
     * JSON be passed, it will simply return and do nothing.
     *
     * @param entityJSON
     */
    public static void runAPICommands(JSONObject entityJSON, String type, Context context) {
        JSONArray apiJSON = EntityUtil.getAPICommands(entityJSON, type);

        if (apiJSON == null)
            return;

        try {
            NagleTimers.reset(
                    entityJSON.getString("entity_id"),
                    new HttpRunnable(entityJSON, apiJSON, context),
                    EntityUtil.getNagleDelay(entityJSON)
            );
        } catch (JSONException ignored) {
        }
    }

    public static boolean isEntityOn(JSONObject entityJSON) {
        String state;

        try {
            state = entityJSON
                    .getJSONObject("new_state")
                    .getString("state");

            state = state.toLowerCase();
        } catch (JSONException | NullPointerException exception) {
            return false;
        }

        return EntityUtil.isConsideredOn(state);
    }

    public static boolean isConsideredOn(String value) {
        return value.equals("on")
                || value.equals("open");
    }

    public static String compileIds(List<JSONObject> entityJSONs) {
        if (entityJSONs == null)
            return "";

        List<String> ids = new ArrayList<>();

        for (JSONObject entityJSON : entityJSONs)
            try {
                ids.add(entityJSON.getString("entity_id"));
            } catch (JSONException ignored) {
            }

        Collections.sort(ids);

        return TextUtils.join(",", ids);
    }


    private static class HttpRunnable implements Runnable {
        private JSONObject entityJSON;
        private JSONArray apiJSON;
        private Context context;

        HttpRunnable(JSONObject entityJSON, JSONArray apiJSON, Context context) {
            this.entityJSON = entityJSON;
            this.apiJSON = apiJSON;
            this.context = context;
        }

        @Override
        public void run() {
            JSONArray execJSON = new JSONArray();

            for (int index = 0, limit = apiJSON.length(); index < limit; index++) {
                JSONObject requestJSON;

                try {
                    try {
                        requestJSON = new JSONObject(apiJSON.getJSONObject(index).toString());

                        if (!APIClient.isValidRequestSpec(requestJSON))
                            throw new JSONException("Invalid JSON request spec");

                        if (requestJSON.has("logic") && !RefStringUtil.processLogic(requestJSON.getString("logic")))
                            continue;

                        if (requestJSON.has("payload"))
                            requestJSON.put(
                                    "payload",
                                    RefStringUtil.processRefs(requestJSON.getString("payload"))
                            );

                        execJSON.put(requestJSON);
                    } catch (JSONException exception) {
                        throw new AvarioException(
                                Constants.ERROR_STATE_API_OBJECTS,
                                exception,
                                new Object[]{
                                        entityJSON.optString("entity_id"),
                                        index
                                }
                        );
                    }
                } catch (AvarioException exception) {
                    if (exception.getCodeValue() == Constants.ERROR_STATE_API_CONDITION) {
                        Object[] args;

                        args = exception.getMessageArguments();
                        args[0] = entityJSON.optString("entity_id");
                        args[1] = index;
                    }

                    Application.mainHandler.post(new PlatformUtil.ErrorToastRunnable(this.context, exception));
                }
            }

            try {
                String entityId = entityJSON.optString("entity_id", null);

                APIClient
                        .getInstance()
                        .sequenceRequests(
                                execJSON,
                                entityId,
                                entityId,
                                new HttpListener(entityId, new String[]{entityId}, this.context)
                        );
            } catch (final AvarioException exception) {
                Application.mainHandler.post(new PlatformUtil.ErrorToastRunnable(this.context, exception));
            }
        }
    }

    private static class HttpListener extends APIMultiListener<String> {
        private Context context;

        HttpListener(String timerId, String[] entityIds, Context context) {
            super(timerId, entityIds);
            this.context = context;
        }

        @Override
        public void onRequestsDone() {
            super.onRequestsDone();

            Application.mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    HttpListener self = HttpListener.this;

                    for (VolleyError error : self.errors) {
                        int code;

                        if (error == null)
                            continue;

                        if (error.networkResponse != null)
                            code = PlatformUtil.responseCodeToErrorCode("1", error.networkResponse.statusCode);
                        else if (error instanceof TimeoutError)
                            code = Constants.ERROR_API_TIMEOUT;
                        else if (error instanceof ParseError)
                            code = Constants.ERROR_API_PARSE;
                        else if (error instanceof NetworkError)
                            code = Constants.ERROR_API_NETWORK;
                        else
                            code = Constants.ERROR_API_HTTP_SERVER;

                        PlatformUtil
                                .getErrorToast(self.context, new AvarioException(code, error))
                                .show();
                    }
                }
            });
        }
    }
}
