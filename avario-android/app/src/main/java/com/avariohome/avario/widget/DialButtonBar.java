package com.avariohome.avario.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIRequestListener;
import com.avariohome.avario.core.Light;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.EntityUtil;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.util.RefStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents the widget that houses the dial buttons that are mapped to specific entities supporting
 * said button
 * <p>
 * TODO swap button may need to be changed to refer from the bootstrap
 * <p>
 * Created by aeroheart-c6 on 03/03/2017.
 */
public class DialButtonBar extends LinearLayout {
    private static final String TIMER_ID = "dial-button";
    private static Pattern buttonPattern = Pattern.compile("^button\\d$");

    private static final int VISIBILITY_ALONE = 1; // show only if it is a single entity
    private static final int VISIBILITY_GROUP = 2; // show only if it is a group of entities
    private static final int VISIBILITY_LIGHTS = 3; // show only if it is a group of entities
    private static final int VISIBILITY_EITHER = VISIBILITY_ALONE | VISIBILITY_GROUP | VISIBILITY_LIGHTS; // show always

    private Map<ImageButton, DialButtonEntity> buttons;
    private Map<JSONObject, DialButtonEntity> entities;
    private WeakReference<Dial> dialref;
    private boolean mediaMode;
    private DialButtonEntity previousEntity;
    private List<String> previousButtons = new ArrayList<>();

    public DialButtonBar(Context context) {
        this(context, null, 0);
    }

    public DialButtonBar(Context context, AttributeSet attributes) {
        this(context, attributes, 0);
    }

    public DialButtonBar(Context context, AttributeSet attributes, int defStyleAttr) {
        super(context, attributes, defStyleAttr);
        this.setOrientation(LinearLayout.HORIZONTAL);

        this.buttons = new LinkedHashMap<>();
        this.entities = new LinkedHashMap<>();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());

        manager.registerReceiver(
                new StateReceiver(),
                new IntentFilter(Constants.BROADCAST_STATE_CHANGED)
        );
    }

    /**
     * Reads into each entity's "dial" property and determine appropriate dial buttons. By default,
     * __always__ assumes that the entity being passed here is not from the media root object
     *
     * @param entityJSONs
     */
    public void setup(List<JSONObject> entityJSONs) {
        this.setup(entityJSONs, false);
    }

    /**
     * Reads into each entity's "dial" property and determine appropriate dial buttons.
     *
     * @param entityJSONs
     * @param mediaMode
     */
    public void setup(List<JSONObject> entityJSONs, boolean mediaMode) {
        ArrayList<Boolean> shouldShowList = new ArrayList<>();
        ArrayList<String> dialTypes = new ArrayList<>();
        this.entities.clear();
        this.buttons.clear();
        shouldShowList.clear();
        this.removeAllViews();
        this.mediaMode = mediaMode;
        boolean isRemoveView = true;
        boolean hasButtons = false;

        for (JSONObject entityJSON : entityJSONs) {
            if (entityJSON.has("dials")) {
                shouldShowList.add(true);
            } else {
                shouldShowList.add(false);
            }

            try {
                if (entityJSON.has("dial")) {
                    dialTypes.add(entityJSON.getJSONObject("dial")
                            .getString("dial_type"));
                } else if (entityJSON.has("dials")) {
                    dialTypes.add(entityJSON.getJSONObject("dials")
                            .getJSONObject("brightness")
                            .getString("dial_type"));
                }
            } catch (JSONException exception) {

            }
        }

        for (JSONObject entityJSON : entityJSONs) {
            try {

                if (mediaMode || (entityJSON.has("dials") && areAllTrue(shouldShowList))) {
                    this.setupEntityMedia(entityJSON);
                } else if (areSameTypes(dialTypes, entityJSONs)) {
                    this.setupEntity(entityJSON);
                }

            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(this.getContext(), exception)
                        .show();
            }
        }

        // setup the buttons for rendering. remove as necessary
        Iterator<JSONObject> iterator = this.entities.keySet().iterator();

        while (iterator.hasNext()) {
            JSONObject buttonJSON = iterator.next();
            DialButtonEntity buttonEntity = this.entities.get(buttonJSON);
            int size = buttonEntity.entityJSONs.size();
            int visibility = buttonJSON.optInt("visibility", DialButtonBar.VISIBILITY_ALONE);

            if (visibility == DialButtonBar.VISIBILITY_EITHER ||
                    visibility == DialButtonBar.VISIBILITY_ALONE && size == 1 ||
                    visibility == DialButtonBar.VISIBILITY_GROUP && size > 1 ||
                    (visibility == DialButtonBar.VISIBILITY_LIGHTS && size >= 1 && entityJSONs.size() == 1)) {
                Log.d("Has buttons itr", hasButton(buttonEntity.buttonJSON.optString("entity_id")) + " ");
                this.setupButton(buttonEntity, hasButton(buttonEntity.buttonJSON.optString("entity_id")));
            } else {
                iterator.remove();
            }
        }

        /*if (areAllTrue(shouldShowList) && areSameTypes(dialTypes) && entityJSONs.size() > 1 && !mediaMode) {
            ImageButton button;

            button = this.generateButton();
            button.setId(R.id.dialbtn__switch);

            AssetUtil.loadImage(
                    this.getContext(),
                    R.array.ic__switch,
                    new AssetUtil.ImageViewCallback(button),
                    button
            );
            if (this.findViewById(R.id.dialbtn__switch) == null) {
                this.addView(button);
            }
            YoYo.with(Techniques.ZoomIn)
                    .interpolate(new BounceInterpolator())
                    .duration(500)
                    .playOn(button);
        }*/

        if (areSameLightTypes(dialTypes) && entityJSONs.size() > 1 && !mediaMode && !areAllTrue(shouldShowList)) {
            DialButtonBar self = DialButtonBar.this;
            Dial dial = self.dialref.get();
            try {
                dial.changeDialType(R.id.dialbtn__brightness);
                dial.initialize();
            } catch (AvarioException e) {
                e.printStackTrace();
            }
            dial.initialize();
        }

        // media mode: add the final button for swapping
        if (this.mediaMode) {
            ImageButton button;

            button = this.generateButton();
            button.setId(R.id.dialbtn__swap);

            AssetUtil.toDrawable(
                    this.getContext(),
                    R.array.ic__dialbtn__dpad,
                    new AssetUtil.ImageViewCallback(button)
            );

            if (this.findViewById(R.id.dialbtn__switch) == null) {
                this.addView(button);
            }

            /*if (!this.mediaMode) {
                YoYo.with(Techniques.ZoomIn)
                        .interpolate(new BounceInterpolator())
                        .duration(500)
                        .playOn(button);
            }*/
        }

        // get reference to the `states` root object (algo state)
        for (ImageButton button : this.buttons.keySet()) {
            DialButtonEntity buttonEntity;
            JSONObject buttonJSON;

            buttonEntity = this.buttons.get(button);
            buttonEntity.entitiesId = EntityUtil.compileIds(buttonEntity.entityJSONs);

            buttonJSON = buttonEntity.buttonJSON;

            if (buttonJSON.has("radio")) {
                try {
                    buttonEntity.stateJSON = StateArray
                            .getInstance()
                            .getDialButtonState(buttonJSON.optString("state_id"))
                            .getJSONObject("states");
                } catch (AvarioException exception) {
                    PlatformUtil
                            .getErrorToast(this.getContext(), exception)
                            .show();
                } catch (JSONException ignored) {
                }
            }
        }

        this.updateStates();
    }

    private boolean areAllTrue(ArrayList<Boolean> array) {
        for (boolean b : array)
            if (!b) return false;
        return true;
    }

    private boolean areSameTypes(ArrayList<String> array, List<JSONObject> entityJSONs) {
        for (int i = 0; i < array.size(); i++) {
            if (!entityJSONs.get(i).has("dials")) {
                return false;
            }

            if (i != 0 && !(array.get(i).equals(array.get(i - 1)))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasButton(String entityId) {
        for (int i = 0; i < previousButtons.size(); i++) {
            if (previousButtons.get(i).equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    private boolean areSameLightTypes(ArrayList<String> array) {
        for (int i = 0; i < array.size(); i++) {
            Log.d("Light types", array.get(0));
            if (!array.get(0).equals("dial.light")) {
                return false;
            }
        }
        return true;
    }

    private void setupEntityMedia(JSONObject entityJSON) throws AvarioException {
        Dial dial = this.dialref.get();
        JSONObject dialspecsJSON,
                dialspecJSON;
        Iterator<String> keys;

        if (dial == null)
            return;

        // Get dials instructions from the media player entity
        try {
            dialspecsJSON = entityJSON.getJSONObject("dials");
            keys = dialspecsJSON.keys();
        } catch (JSONException exception) {
            String[] msgArgs = new String[]{String.format(
                    "%s.dials",
                    entityJSON.optString("entity_id")
            )};

            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    msgArgs
            );
        }

        // Select appropriate dial type to render
        dialspecJSON = null;

        while (keys.hasNext()) {
            String dialId = dial.getType().getId(),
                    key = keys.next();

            try {
                dialspecJSON = dialspecsJSON.getJSONObject(key);

                if (dialId.equals(dialspecJSON.getString("dial_type")))
                    break;
            } catch (JSONException exception) {
                String[] msgArgs = new String[]{String.format(
                        "%s.dials.%s.dial_type",
                        entityJSON.optString("entity_id"),
                        key
                )};

                throw new AvarioException(
                        Constants.ERROR_STATE_MISSINGKEY,
                        exception,
                        msgArgs
                );
            }
        }

        this.setupFromSpec(dialspecJSON, entityJSON);
    }

    /**
     * "Registers" a entity into the widget. This effectively maps the entity to respond to only to
     * this button.
     */
    private void setupEntity(JSONObject entityJSON) throws AvarioException {

        try {
            if (entityJSON.has("dial")) {
                this.setupFromSpec(entityJSON.getJSONObject("dial"), entityJSON);
            } else if (entityJSON.has("dials")) {
                this.setupFromSpec(entityJSON.getJSONObject("dials").getJSONObject("brightness"), entityJSON);
            }
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{String.format("%s.dial", entityJSON.optString("entity_id"))}
            );
        }
    }

    private void setupFromSpec(JSONObject dialspecJSON, JSONObject entityJSON) throws AvarioException {
        StateArray states = StateArray.getInstance();
        Iterator<String> keys = dialspecJSON.keys();

        String property = null;

        try {
            while (keys.hasNext()) {
                property = keys.next();

                if (!DialButtonBar.buttonPattern.matcher(property).matches())
                    continue;

                JSONObject buttonJSON = states.getDialButton(dialspecJSON.getString(property));
                DialButtonEntity meta;

                if (!this.entities.containsKey(buttonJSON)) {
                    meta = new DialButtonEntity();
                    meta.buttonJSON = buttonJSON;

                    this.entities.put(buttonJSON, meta);
                } else
                    meta = this.entities.get(buttonJSON);

                meta.entityJSONs.add(entityJSON);
                meta.stateRef = dialspecJSON.optString(property + "_state", null);
            }
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("%s.dial.%s",
                                    entityJSON.optString("entity_id"),
                                    property
                            )
                    }
            );
        }
    }

    /**
     * Maps an ImageButton instance to the JSONObject describing the button in action. This will
     * return immediately if said button has already been created before.
     *
     * @param buttonEntity
     */
    private void setupButton(DialButtonEntity buttonEntity, boolean hasButton) {
        ImageButton button = this.generateButton();

        int id = 0;

        switch (buttonEntity.buttonJSON.optString("entity_id")) {
            case "colour":
                id = R.id.dialbtn__colour;
                break;
            case "saturation":
                id = R.id.dialbtn__saturation;
                break;
            case "brightness":
                id = R.id.dialbtn__brightness;
                break;
            case "temperature":
                id = R.id.dialbtn__temperature;
                break;
            case "lightalgo1":
                id = R.id.dialbtn__algo1;
                break;
            case "lightalgo2":
                id = R.id.dialbtn__algo2;
                break;
            case "lightalgo3":
                id = R.id.dialbtn__algo3;
                break;
            case "home":
                id = R.id.dialbtn__home;
                break;
            case "back":
                id = R.id.dialbtn__back;
                break;
            case "menu":
                id = R.id.dialbtn__menu;
                break;
        }

        button.setId(id);
        button.setTag(buttonEntity.buttonJSON.optString("entity_id"));
        this.buttons.put(button, buttonEntity);
        this.addView(button);
        Log.d("Has button bellow", hasButton + "");
        /*if (!hasButton &&
                !this.mediaMode) {
            YoYo.with(Techniques.ZoomIn)
                    .interpolate(new BounceInterpolator())
                    .duration(500)
                    .playOn(button);
        }*/
        previousButtons.add(buttonEntity.buttonJSON.optString("entity_id"));

        if (!previousButtons.isEmpty() && previousButtons.size() >= 6) {
            previousButtons.remove(0);
            previousButtons.remove(0);
            previousButtons.remove(0);
        }
    }

    private ImageButton generateButton() {
        Resources res = this.getResources();

        ImageButton button;
        LayoutParams params;

        params = new LayoutParams(
                res.getDimensionPixelSize(R.dimen.dialbutton__width),
                res.getDimensionPixelSize(R.dimen.dialbutton__height)
        );
        params.setMargins(
                this.buttons.isEmpty() ? 0 : res.getDimensionPixelSize(R.dimen.dialbutton__margin__left),
                0, 0, 0
        );

        int padding = res.getDimensionPixelSize(R.dimen.dialbutton__padding);

        button = new ImageButton(this.getContext(), null, 0);
        button.setLayoutParams(params);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageView.ScaleType.FIT_XY);
        button.setOnTouchListener(new TouchListener(button));

        AssetUtil.toDrawable(
                this.getContext(),
                R.array.bg__dialbtn,
                new AssetUtil.BackgroundCallback(button)
        );

        return button;
    }

    private void updateStates() {
        try {
            for (ImageButton button : this.buttons.keySet())
                this.updateButtonState(button);
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();
        }
    }

    private void updateButtonState(ImageButton button) throws AvarioException {
        DialButtonEntity buttonEntity = this.buttons.get(button);
        JSONObject buttonJSON = buttonEntity.buttonJSON;

        try {
            String iconURL;

            if (buttonJSON.has("multi")) {
                JSONArray iconsJSON;
                int level;

                try {
                    level = Integer.parseInt(RefStringUtil.processRefs(buttonEntity.stateRef));
                    level = level % buttonJSON.getJSONArray("multi").length();
                } catch (NullPointerException | NumberFormatException exception) {
                    level = 0;
                }

                iconsJSON = buttonJSON.getJSONArray("icon_par");
                iconURL = iconsJSON.getString(level);
            } else if (buttonJSON.has("radio")) {
                String stateActive = buttonJSON.getString("active_state"),
                        state = this.resolveRadioState(
                                buttonJSON,
                                buttonJSON.getString("state"),
                                buttonEntity.entitiesId
                        );

                /*iconURL = buttonJSON.getString(
                    state != null && state.equals(stateActive)
                        ? "icon_on"
                        : "icon_off"
                );*/

                Light.addAlgo(buttonEntity.entitiesId,
                        buttonJSON.getJSONObject("controls")
                                .getJSONObject("api")
                                .getJSONObject("clk")
                                .getString("payload"));

                iconURL = buttonJSON.getString(
                        Light.isStateSame(
                                buttonEntity.entitiesId, stateActive)
                                ? "icon_on"
                                : "icon_off");
            } else
                iconURL = buttonJSON.getString("icon_off");

            AssetUtil.toDrawable(
                    this.getContext(),
                    AssetUtil.toAbsoluteURLs(this.getContext(), new String[]{iconURL}),
                    new AssetUtil.ImageViewCallback(button)
            );
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new String[]{buttonJSON.optString("entity_id")}
            );
        }
    }

    private String resolveRadioState(JSONObject btnspecJSON, String stateString, String id) {
        StateArray states = StateArray.getInstance();

        List<String> keys = new ArrayList<>();
        Map<String, String> mapping = new HashMap<>();

        Matcher matcher = RefStringUtil.extractMarkers(stateString, keys);
        JSONObject stateJSON;

        try {
            stateJSON = states.getDialButtonStates();
        } catch (AvarioException exception) {
            return null;
        }

        try {
            if (!keys.isEmpty()) {
                String key = keys.get(0);

                mapping.put(
                        key,
                        RefStringUtil.resolveValue(stateJSON, key + "/" + PlatformUtil.hashMD5(id))
                );
            }

            return RefStringUtil.replaceMarkers(matcher, mapping);
        } catch (AvarioException exception) {
            return keys.size() > 0
                    ? this.registerRadioState(keys.get(0), id, btnspecJSON.optString("active_state"))
                    : null;
        }
    }

    private String registerRadioState(String path, String id, String value) {
        String[] parts = path.split("/");
        JSONObject rootJSON;
        StateArray states;

        if (parts.length <= 1)
            return null;

        states = StateArray.getInstance();

        try {
            rootJSON = states.getDialButtonStates();
        } catch (AvarioException exception) {
            return null;
        }

        try {
            JSONObject objectJSON = rootJSON;

            for (int index = 0, length = parts.length; index < length; index++)
                objectJSON = objectJSON.getJSONObject(parts[index]);

            objectJSON.put(PlatformUtil.hashMD5(id), value);
            states.save();
        } catch (AvarioException | JSONException exception) {
        }

        return value;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled)
            this.removeAllViews();
        else
            this.setup(new ArrayList<JSONObject>(), this.mediaMode);
    }

    public void setDial(Dial dial) {
        this.dialref = new WeakReference<>(dial);
    }

    /*
     ***********************************************************************************************
     * Inner Classes
     ***********************************************************************************************
     */
    private class NagleRunnable implements Runnable {
        private JSONObject buttonspecJSON;
        private JSONObject requestspecJSON;

        public NagleRunnable(JSONObject buttonspecJSON, JSONObject requestspecJSON) {
            this.buttonspecJSON = buttonspecJSON;
            this.requestspecJSON = requestspecJSON;
        }

        @Override
        public void run() {
            try {
                DialButtonEntity meta = DialButtonBar.this.entities.get(this.buttonspecJSON);

                APIClient
                        .getInstance()
                        .executeRequest(
                                this.requestspecJSON,
                                this.buttonspecJSON.optString("entity_id"),
                                DialButtonBar.TIMER_ID,
                                new APIListener(meta.entitiesId.split(","))
                        );

                this.runRadioButton();
            } catch (final AvarioException exception) {
                Application.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        PlatformUtil
                                .getErrorToast(DialButtonBar.this.getContext(), exception)
                                .show();
                    }
                });
            }
        }

        private void runRadioButton() throws AvarioException {
            DialButtonBar self = DialButtonBar.this;
            DialButtonEntity meta = self.entities.get(this.buttonspecJSON);

            if (!this.buttonspecJSON.has("radio"))
                return;

            try {
                List<String> keys = new ArrayList<>();

                RefStringUtil.extractMarkers(
                        this.buttonspecJSON.getString("state"),
                        keys
                );

                for (String key : keys)
                    self.registerRadioState(
                            key,
                            meta.entitiesId,
                            this.buttonspecJSON.getString("active_state")
                    );
            } catch (JSONException exception) {
                throw new AvarioException(
                        Constants.ERROR_STATE_MISSINGKEY,
                        exception,
                        new String[]{String.format(
                                "%s.%s,%s.%s",
                                this.buttonspecJSON.optString("entity_id"), "active_state",
                                this.buttonspecJSON.optString("entity_id"), "state"
                        )}
                );
            }
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Listeners
     ***********************************************************************************************
     */
    private class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DialButtonBar self = DialButtonBar.this;
            StateArray states = StateArray.getInstance();

            String extraId = intent.getStringExtra("entity_id");

            // reset mappings of button entities
            for (ImageButton button : self.buttons.keySet()) {
                DialButtonEntity buttonEntity = self.buttons.get(button);
                List<JSONObject> entityJSONs;
                JSONObject btnJSON,
                        tmpJSON;

                try {
                    tmpJSON = buttonEntity.buttonJSON;
                    btnJSON = states.getDialButton(tmpJSON.optString("entity_id"));
                } catch (AvarioException ignored) {
                    continue;
                }

                buttonEntity = self.entities.remove(tmpJSON);
                buttonEntity.buttonJSON = btnJSON;

                self.buttons.put(button, buttonEntity);
                self.entities.put(btnJSON, buttonEntity);

                entityJSONs = new ArrayList<>();

                if (extraId == null) {
                    for (JSONObject entityJSON : buttonEntity.entityJSONs)
                        try {
                            entityJSONs.add(states.getEntity(entityJSON.optString("entity_id")));
                        } catch (AvarioException ignored) {
                        }
                } else {
                    for (JSONObject entityJSON : buttonEntity.entityJSONs) {
                        if (!extraId.equals(entityJSON.optString("entity_id")))
                            entityJSONs.add(entityJSON);
                        else
                            try {
                                entityJSONs.add(states.getEntity(entityJSON.optString("entity_id")));
                            } catch (AvarioException ignored) {
                            }
                    }
                }
            }

            self.updateStates();
        }
    }

    private class APIListener extends APIRequestListener<String> {
        public APIListener(String[] entityIds) {
            super(DialButtonBar.TIMER_ID, entityIds);
        }

    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private ImageButton source;

        private GestureListener(ImageButton source) {
            this.source = source;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            Log.d("ID", source.getId() + "");
            int id = this.source.getId();
            switch (this.source.getId()) {
                case R.id.dialbtn__swap:
                    this.handleSwap();
                    return true;
                case R.id.dialbtn__switch:
                    this.swapDialLights();
                    return true;
                case R.id.dialbtn__colour:
                    this.swapDial(source.getId());
                    return true;
                case R.id.dialbtn__saturation:
                    this.swapDial(source.getId());
                    return true;
                case R.id.dialbtn__brightness:
                    this.swapDial(source.getId());
                    return true;
                case R.id.dialbtn__temperature:
                    this.swapDial(source.getId());
                    return true;
                case R.id.dialbtn__home:
                    this.handleMediaPad("clk");
                    return true;
                case R.id.dialbtn__back:
                    this.handleMediaPad("clk");
                    return true;
                case R.id.dialbtn__menu:
                    this.handleMediaPad("clk");
                    return true;
            }

            this.handle("clk");
            return super.onSingleTapConfirmed(event);
        }

        @Override
        public void onLongPress(MotionEvent event) {
            if (this.source.getId() == R.id.dialbtn__swap)
                return;

            this.handle("lng");
            super.onLongPress(event);
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (this.source.getId() == R.id.dialbtn__swap)
                return false;

            this.handle("dbl");
            return super.onDoubleTap(event);
        }

        private void handleSwap() {
            DialButtonBar self = DialButtonBar.this;
            Dial dial = self.dialref.get();

            try {
                dial.swapMediaDialType();
                self.setup(dial.getEntities(), true);

                dial.initialize();
            } catch (NullPointerException ignored) {
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(DialButtonBar.this.getContext(), exception)
                        .show();
            }
        }

        private void setUpSwapLights() {
            com.avariohome.avario.util.Log.d("Swap", "setUpSwapLights");
            DialButtonBar self = DialButtonBar.this;
            Dial dial = self.dialref.get();
            List<JSONObject> entityJSONs = dial.getEntities();
            removeAllViews();
            entities.clear();
            boolean hasButtons = false;

            if (!isDialAlgo()) {
                buttons.clear();
                for (JSONObject entityJSON : entityJSONs) {
                    try {
                        setupEntity(entityJSON);
                    } catch (AvarioException exception) {
                        PlatformUtil
                                .getErrorToast(getContext(), exception)
                                .show();
                    }
                }

                // setup the buttons for rendering. remove as necessary
                Iterator<JSONObject> iterator = entities.keySet().iterator();

                while (iterator.hasNext()) {
                    JSONObject buttonJSON = iterator.next();
                    DialButtonEntity buttonEntity = entities.get(buttonJSON);
                    setupButton(buttonEntity, hasButton(buttonEntity.buttonJSON.optString("entity_id")));
                }

                /*ImageButton button;

                button = generateButton();
                button.setId(R.id.dialbtn__switch);

                AssetUtil.loadImage(
                        getContext(),
                        R.array.ic__switch,
                        new AssetUtil.ImageViewCallback(button),
                        button
                );

                if (findViewById(R.id.dialbtn__switch) == null) {
                    addView(button);
                }*/
            } else {
                switch (dial.getType()) {
                    case LIGHT:
                        swapDial(R.id.dialbtn__brightness);
                        break;
                    case COLOUR:
                        swapDial(R.id.dialbtn__colour);
                        break;
                    case SATURATION:
                        swapDial(R.id.dialbtn__saturation);
                        break;
                    case TEMPERATURE:
                        swapDial(R.id.dialbtn__temperature);
                        break;
                }
            }
            for (ImageButton button : buttons.keySet()) {
                DialButtonEntity buttonEntity;
                JSONObject buttonJSON;

                buttonEntity = buttons.get(button);
                buttonEntity.entitiesId = EntityUtil.compileIds(buttonEntity.entityJSONs);

                buttonJSON = buttonEntity.buttonJSON;

                if (buttonJSON.has("radio")) {
                    try {
                        buttonEntity.stateJSON = StateArray
                                .getInstance()
                                .getDialButtonState(buttonJSON.optString("state_id"))
                                .getJSONObject("states");
                    } catch (AvarioException exception) {
                        PlatformUtil
                                .getErrorToast(getContext(), exception)
                                .show();
                    } catch (JSONException ignored) {
                    }
                }
            }

            updateStates();
        }


        private boolean isDialAlgo() {
            for (Map.Entry<ImageButton, DialButtonEntity> item : buttons.entrySet()) {
                if (item.getKey().getId() == R.id.dialbtn__colour ||
                        item.getKey().getId() == R.id.dialbtn__brightness ||
                        item.getKey().getId() == R.id.dialbtn__saturation ||
                        item.getKey().getId() == R.id.dialbtn__temperature) {
                    return false;
                }

            }
            return true;
        }

        private void swapDial(int id) {
            com.avariohome.avario.util.Log.d("Swap", "swapDial");
            DialButtonBar self = DialButtonBar.this;
            Dial dial = self.dialref.get();

            try {
                dial.changeDialType(id);
                self.setup(dial.getEntities(), false);

                dial.initialize();
            } catch (NullPointerException ignored) {
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(DialButtonBar.this.getContext(), exception)
                        .show();
            }
        }

        private void swapDialLights() {
            com.avariohome.avario.util.Log.d("Swap", "swapDialLights");
            DialButtonBar self = DialButtonBar.this;
            Dial dial = self.dialref.get();

            try {
                dial.changeDialType(R.id.dialbtn__brightness);
                setUpSwapLights();

                dial.initialize();
            } catch (NullPointerException ignored) {
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(DialButtonBar.this.getContext(), exception)
                        .show();
            }
        }

        private void handle(String type) {
            DialButtonBar self = DialButtonBar.this;
            DialButtonEntity buttonEntity = self.buttons.get(this.source);
            if (buttonEntity == null) {
                return;
            }
            JSONObject buttonJSON = buttonEntity.buttonJSON,
                    requestJSON;

            try {
                requestJSON = this.getRequestSpec(buttonJSON, type);

                if (requestJSON == null)
                    return;

                this.processRequestSpec(requestJSON, buttonEntity);

                try {
                    Light.updateAlgo(buttonEntity.entitiesId,
                            buttonJSON.getJSONObject("controls")
                                    .getJSONObject("api")
                                    .getJSONObject("clk")
                                    .getString("payload"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                updateStates();
//                NagleTimers.reset(
//                    DialButtonBar.TIMER_ID,
//                    new NagleRunnable(buttonJSON, requestJSON),
//                    EntityUtil.getNagleDelay(buttonEntity.buttonJSON)
//                );
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(self.getContext(), exception)
                        .show();
            }
        }

        private void handleMediaPad(String type) {
            DialButtonBar self = DialButtonBar.this;
            DialButtonEntity buttonEntity = self.buttons.get(this.source);
            if (buttonEntity == null) {
                return;
            }
            JSONObject buttonJSON = buttonEntity.buttonJSON,
                    requestJSON;

            try {
                requestJSON = this.getRequestSpec(buttonJSON, type);

                if (requestJSON == null)
                    return;

                this.processRequestSpec(requestJSON, buttonEntity);

                try {
                    Light.updateAlgo(buttonEntity.entitiesId,
                            buttonJSON.getJSONObject("controls")
                                    .getJSONObject("api")
                                    .getJSONObject("clk")
                                    .getString("payload"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                DialButtonEntity meta = DialButtonBar.this.entities.get(buttonJSON);

                APIClient.getInstance().executeRequest(
                        requestJSON,
                        new APIListener(meta.entitiesId.split(","))
                );

                updateStates();
//                NagleTimers.reset(
//                    DialButtonBar.TIMER_ID,
//                    new NagleRunnable(buttonJSON, requestJSON),
//                    EntityUtil.getNagleDelay(buttonEntity.buttonJSON)
//                );
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(self.getContext(), exception)
                        .show();
            }
        }

        private JSONObject getRequestSpec(JSONObject btnspecJSON, String type) throws AvarioException {
            JSONObject rqspecJSON;

            try {
                rqspecJSON = btnspecJSON
                        .getJSONObject("controls")
                        .getJSONObject("api");
            } catch (JSONException exception) {
                throw new AvarioException(
                        Constants.ERROR_STATE_MISSINGKEY,
                        exception,
                        new Object[]{
                                String.format("%s.controls.api", btnspecJSON.optString("entity_id"))
                        }
                );
            }

            try {
                return new JSONObject(
                        rqspecJSON
                                .getJSONObject(type)
                                .toString()
                );
            } catch (JSONException exception) {
                return null;
            }
        }

        private void processRequestSpec(JSONObject specJSON, DialButtonEntity meta) throws AvarioException {
            if (!APIClient.isValidRequestSpec(specJSON))
                throw new AvarioException(
                        Constants.ERROR_STATE_API_OBJECTS,
                        null,
                        new Object[]{specJSON.optString("entity_id"), 0}
                );

            try {
                Dial dial = DialButtonBar.this.dialref.get();
                Matcher matcher = RefStringUtil.extractMarkers(specJSON.getString("payload"), null);
                Map<String, String> mapping;

                mapping = new HashMap<>();
                mapping.put("entity_ids", meta.entitiesId);

                if (dial != null)
                    mapping.put("value", String.valueOf(dial.getValue()));

                specJSON.put("payload", RefStringUtil.replaceMarkers(matcher, mapping));
            } catch (JSONException ignored) {
            }
        }
    }


    private class TouchListener implements View.OnTouchListener {
        private GestureDetector detector;

        private TouchListener(ImageButton source) {
            DialButtonBar self = DialButtonBar.this;
            GestureListener listener = new GestureListener(source);

            this.detector = new GestureDetector(self.getContext(), listener);
            this.detector.setOnDoubleTapListener(listener);
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            return this.detector.onTouchEvent(event);
        }
    }

    /**
     * Initiate on touch event to selected dial button.
     * Avoid hard coding api request and minimize code complexity.
     *
     * @param entityId button entity id
     */
    public void click(String entityId) {
        for (Map.Entry<ImageButton, DialButtonEntity> item : buttons.entrySet()) {
            try {
                if (item.getValue().entitiesId.equals(entityId)
                        && Light.isStateSame(item.getValue().entitiesId,
                        item.getValue().buttonJSON.getString("active_state"))) {
                    long downTime = SystemClock.uptimeMillis();
                    long eventTime = SystemClock.uptimeMillis() + 100;
                    float x = 0.0f;
                    float y = 0.0f;

                    int metaState = 0;
                    MotionEvent motionEvent = MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_DOWN,
                            x,
                            y,
                            metaState
                    );
                    item.getKey().dispatchTouchEvent(motionEvent);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class DialButtonEntity {
        /**
         * The consolidated entitiesId string of entityJSONs. Examples would be:
         * > light.side
         * > light.main,light.side
         * <p>
         * This field will be alphabetically arranged
         */
        String entitiesId;
        List<JSONObject> entityJSONs;

        /**
         * A reference to the object residing in the state entity object in the StateArray
         * mega JSON (e.g. light_algo/states JSONObject).
         */
        JSONObject stateJSON;

        /**
         * A reference to the object that represents the dial button JSON object in the StateArray
         */
        JSONObject buttonJSON;

        /**
         * Non-null ONLY when button is multi-type because this is a ref string to the value of the
         * fan
         */
        String stateRef;

        DialButtonEntity() {
            this.entityJSONs = new ArrayList<>();
        }
    }

    /*
        Code is too complex and not enough documentation.
     */
}
