package com.avariohome.avario.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIRequestListener;
import com.avariohome.avario.api.models.ColorBody;
import com.avariohome.avario.api.models.SaturationBody;
import com.avariohome.avario.api.models.TemperatureBody;
import com.avariohome.avario.core.APITimers;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.Light;
import com.avariohome.avario.core.NagleTimers;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.DrawableLoader;
import com.avariohome.avario.util.EntityUtil;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.util.RefStringUtil;
import com.google.gson.Gson;
import com.triggertrap.seekarc.SeekArc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;


/**
 * View to show the different dial controls depending on selected entities.
 * <p>
 * Created by aeroheart-c6 on 08/03/2017.
 */
public class Dial extends FrameLayout {
    public enum Category {
        ENTITY,
        MEDIA,
        VOLUME,
        COLOUR,
        SATURATION,
        BRIGHTNESS,
        TEMPRATURE
    }

    public enum Type {
        SWITCH("dial.switch"),
        LIGHT("dial.light"),
        VOLUME("dial.volume"),
        THERMO("dial.thermo"),
        COVER("dial.cover"),
        MEDIASEEK("dial.mediaseek"),
        MEDIANS("dial.medians"), // media no-seek
        MEDIADPAD("dial.mediapad"),
        SATURATION("dial.saturation"),
        COLOUR("dial.colour"),
        TEMPERATURE("dial.temprature");

        /**
         * Iterates through the available values to find a match. Should there be no
         * match, returns the SWITCH type
         *
         * @param id the dial entity ID as described in the bootstrap JSON file
         * @return the actual Type object
         */
        public static Type fromId(String id) {
            for (Type type : Type.values())
                if (type.id.equals(id))
                    return type;

            return Type.SWITCH;
        }

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    public static final int SOURCE_SETUP = 0;
    public static final int SOURCE_USER = 1;
    public static final int SOURCE_MQTT = 2;
    public static final int SOURCE_TIME = 4;

    private static final String TAG = "Avario/Dial";
    private static final String TIMER_ID = "dial";

    private int color;

    private SeekArc arc;

    // Switch type fields
    private FrameLayout switchHolder;
    private ImageButton switchPowerIB;

    // Light type fields
    private FrameLayout lightHolder;
    private ImageButton lightPowerIB;
    private TextView lightPercentTV;

    // Colour type fields
    private FrameLayout colourHolder;
    private ImageButton colourPowerIB;
    private TextView colourPercentTV;

    // Saturation type fields
    private FrameLayout saturationHolder;
    private ImageButton saturationPowerIB;
    private TextView saturationPercentTV;

    // Temprature type fields
    private FrameLayout tempratureHolder;
    private ImageButton tempraturePowerIB;
    private TextView tempraturePercentTV;

    // Volume type fields
    private FrameLayout volumeHolder;
    private ImageButton volumePowerIB;
    private TextView volumePercentTV;

    // Thermo type fields
    private FrameLayout thermoHolder;
    private ImageButton thermoPowerIB;
    private TextView thermoPercentTV;

    // Cover type fields
    private FrameLayout coverHolder;
    private FrameLayout coverCtrlHolder;
    private TextView coverPercentTV;
    private TextView coverOpenTV;
    private TextView coverCloseTV;

    // MediaSeek type fields
    private RelativeLayout mediaseekHolder;
    private TextView mediaseekCurrentTV;
    private TextView mediaseekRemainTV;

    // MediaDpad type fields
    private RelativeLayout mediadpadHolder;
    private ImageButton mediadpadCenterIB;
    private ImageButton mediadpadUpIB;
    private ImageButton mediadpadDownIB;
    private ImageButton mediadpadLeftIB;
    private ImageButton mediadpadRightIB;

    private List<JSONObject> entities;
    private List<String> entitiesUpdated; // used for determining if all entities represented in the dial has already received an MQTT update
    private JSONObject dialJSON;
    private String entitiesId;
    private Category category;
    private Type type;
    private int progressPrev;
    private boolean entitiesChanged; // currently only used for adaptMedia()
    private boolean expectsMQTT;

    private Handler mainHandler;
    private IdleRunnable idleRunnable;
    private HoldRunnable holdRunnable;

    private boolean isLightsOn = false;

    public Dial(Context context) {
        this(context, null, 0);
    }

    public Dial(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Dial(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater;

        inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(R.layout.dial, this);

        this.entitiesChanged = false;
        this.progressPrev = 100;
        this.mainHandler = Application.mainHandler;
        this.idleRunnable = new IdleRunnable();
        this.holdRunnable = new HoldRunnable();

        this.arc = (SeekArc) this.findViewById(R.id.arc);
        this.arc.setOnSeekArcChangeListener(new SeekArcChangeListener());

        this.bootAccessories();
        this.bootAssets();

        LocalBroadcastManager manager;

        manager = LocalBroadcastManager.getInstance(this.getContext());
        manager.registerReceiver(new StateReceiver(), new IntentFilter(Constants.BROADCAST_STATE_CHANGED));
        manager.registerReceiver(new TickerReceiver(), new IntentFilter(Constants.BROADCAST_MEDIA));
    }

    // region Initialization
    private void bootAccessories() {
        AccessoryClickListener clickListener = new AccessoryClickListener();
        AccessoryTouchListener touchListener = new AccessoryTouchListener();
        View holder;

        holder = this.switchHolder = (FrameLayout) this.findViewById(R.id.acc__switch__holder);
        this.switchPowerIB = (ImageButton) holder.findViewById(R.id.switch__btn);

        this.switchPowerIB.setOnClickListener(clickListener);

        holder = this.lightHolder = (FrameLayout) this.findViewById(R.id.acc__light__holder);
        this.lightPercentTV = (TextView) holder.findViewById(R.id.light__percent);
        this.lightPowerIB = (ImageButton) holder.findViewById(R.id.light__btn);

        this.lightPowerIB.setOnClickListener(clickListener);

        holder = this.colourHolder = (FrameLayout) this.findViewById(R.id.acc__colour__holder);
        this.colourPercentTV = (TextView) this.findViewById(R.id.colour__percent);
        this.colourPowerIB = (ImageButton) holder.findViewById(R.id.colour__btn);

        this.colourPowerIB.setOnClickListener(clickListener);

        holder = this.saturationHolder = (FrameLayout) this.findViewById(R.id.acc__saturation__holder);
        this.saturationPercentTV = (TextView) this.findViewById(R.id.saturation__percent);
        this.saturationPowerIB = (ImageButton) holder.findViewById(R.id.saturation__btn);

        this.saturationPowerIB.setOnClickListener(clickListener);

        holder = this.tempratureHolder = (FrameLayout) this.findViewById(R.id.acc__temprature__holder);
        this.tempraturePercentTV = (TextView) this.findViewById(R.id.tempreature__percent);
        this.tempraturePowerIB = (ImageButton) holder.findViewById(R.id.temprature__btn);

        this.tempraturePowerIB.setOnClickListener(clickListener);

        holder = this.volumeHolder = (FrameLayout) this.findViewById(R.id.acc__volume__holder);
        this.volumePercentTV = (TextView) holder.findViewById(R.id.volume__percent);
        this.volumePowerIB = (ImageButton) holder.findViewById(R.id.volume__btn);

        this.volumePowerIB.setOnClickListener(clickListener);

        holder = this.thermoHolder = (FrameLayout) this.findViewById(R.id.acc__thermo__holder);
        this.thermoPercentTV = (TextView) holder.findViewById(R.id.thermo__percent);
        this.thermoPowerIB = (ImageButton) holder.findViewById(R.id.thermo__btn);

        this.thermoPowerIB.setOnClickListener(clickListener);

        holder = this.coverHolder = (FrameLayout) this.findViewById(R.id.acc__cover__holder);
        this.coverCtrlHolder = (FrameLayout) holder.findViewById(R.id.cover__controls__holder);
        this.coverOpenTV = (TextView) holder.findViewById(R.id.cover__open);
        this.coverCloseTV = (TextView) holder.findViewById(R.id.cover__close);
        this.coverPercentTV = (TextView) holder.findViewById(R.id.cover__percent);

        this.coverOpenTV.setOnClickListener(clickListener);
        this.coverCloseTV.setOnClickListener(clickListener);

        holder = this.mediaseekHolder = (RelativeLayout) this.findViewById(R.id.acc__media__holder);
        this.mediaseekCurrentTV = (TextView) holder.findViewById(R.id.mediaseek__current);
        this.mediaseekRemainTV = (TextView) holder.findViewById(R.id.mediaseek__remaining);

        holder = this.mediadpadHolder = (RelativeLayout) this.findViewById(R.id.acc__dpad__holder);
        this.mediadpadCenterIB = (ImageButton) holder.findViewById(R.id.dpad__center);
        this.mediadpadUpIB = (ImageButton) holder.findViewById(R.id.dpad__up);
        this.mediadpadDownIB = (ImageButton) holder.findViewById(R.id.dpad__down);
        this.mediadpadLeftIB = (ImageButton) holder.findViewById(R.id.dpad__left);
        this.mediadpadRightIB = (ImageButton) holder.findViewById(R.id.dpad__right);

        this.mediadpadCenterIB.setOnTouchListener(touchListener);
        this.mediadpadUpIB.setOnTouchListener(touchListener);
        this.mediadpadDownIB.setOnTouchListener(touchListener);
        this.mediadpadLeftIB.setOnTouchListener(touchListener);
        this.mediadpadRightIB.setOnTouchListener(touchListener);
    }

    private void bootAssets() {
        Context context = this.getContext();

        AssetUtil.toDrawable(
                context, R.array.bg__dial__base,
                new AssetUtil.BackgroundCallback(this)
        );

        // our default disabled asset
        AssetUtil.toDrawable(
                this.getContext(),
                R.array.bg__dial__top__flat,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        // Switch Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.switchPowerIB)
        );

        // Light Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.lightPowerIB)
        );

        // Colour Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.colourPowerIB)
        );

        // Saturation Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.saturationPowerIB)
        );

        // Temprature Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.tempraturePowerIB)
        );

        // Volume Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.volumePowerIB)
        );

        // Thermo Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__power,
                new AssetUtil.ImageViewCallback(this.thermoPowerIB)
        );

        // MediaDpad Dial
        AssetUtil.toDrawable(
                context, R.array.ic__dial__dpad__center,
                new AssetUtil.ImageViewCallback(this.mediadpadCenterIB)
        );

        AssetUtil.toDrawable(
                context, R.array.ic__dial__dpad__up,
                new AssetUtil.ImageViewCallback(this.mediadpadUpIB)
        );

        AssetUtil.toDrawable(
                context, R.array.ic__dial__dpad__down,
                new AssetUtil.ImageViewCallback(this.mediadpadDownIB)
        );

        AssetUtil.toDrawable(
                context, R.array.ic__dial__dpad__left,
                new AssetUtil.ImageViewCallback(this.mediadpadLeftIB)
        );

        AssetUtil.toDrawable(
                context, R.array.ic__dial__dpad__right,
                new AssetUtil.ImageViewCallback(this.mediadpadRightIB)
        );
    }

    // endregion
    public Type getType() {
        return this.type;
    }

    public List<JSONObject> getEntities() {
        return this.entities;
    }

    public int getValue() {
        return this.arc.getValue();
    }

    private int getNagleDelay() {
        Config config = Config.getInstance();

        return this.dialJSON.optInt(
                "nagle",
                this.category == Category.ENTITY ? config.getNagleDelay() : config.getNagleMediaDelay()
        );
    }

    private int getMediaDuration() {
        if (this.type != Type.MEDIASEEK || this.entities == null || this.entities.isEmpty())
            return -1;

        JSONObject entityJSON = this.entities.get(0);

        try {
            return (int) Math.round(
                    entityJSON
                            .getJSONObject("new_state")
                            .getJSONObject("attributes")
                            .getDouble("media_duration")
            );
        } catch (JSONException e) {
            AvarioException exception = new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY, e,
                    new Object[]{
                            String.format("%s.new_state.attributes.media_duration", entityJSON.optString("entity_id"))
                    }
            );

            PlatformUtil.logError(exception);

            return -1;
        }
    }

    /**
     * Returns the general state for the switch dial according to entities' value property
     *
     * @return the state of the switch whether or not it's "on" or "off"
     */
    private String getStateSwitch() {
        if (this.type != Type.SWITCH)
            return null;

        String state = this.entities.isEmpty()
                ? "off"
                : "on";

        for (JSONObject entityJSON : this.entities) {
            String value,
                    stateTmp;

            try {
                value = RefStringUtil.processCode(entityJSON.optString("value"));
            } catch (AvarioException exception) {
                value = "off";
            }

            try {
                stateTmp = Float.parseFloat(value) <= 0 ? "off" : "on";
            } catch (NumberFormatException exception) {
                stateTmp = EntityUtil.isConsideredOn(value) ? "on" : "off";
            }

            if (stateTmp.equals("off"))
                state = stateTmp;

            if (state.equals("off"))
                break;
        }

        return state;
    }

    /**
     * Returns the general state for the light dial according to entities' brightness levels.
     * Particularly useful on multiple entities. This is currently used in
     * <p>
     * Brightness level is read from {{ entity_id/new_state/attributes/brightness }}
     *
     * @return the state of the light whether or not it's "on" or "off"
     */
    private String getStateLight() {
        if (this.type != Type.LIGHT && this.type != Type.COLOUR && this.type != Type.SATURATION
                && this.type != Type.TEMPERATURE)
            return null;

        String state = this.entities.isEmpty()
                ? "off"
                : null;

        for (JSONObject entityJSON : this.entities) {
            String stateTmp;
            int brightness;

            try {
                brightness = (int) Float.parseFloat(RefStringUtil.processCode(
                        entityJSON.optString("value")
                ));
            } catch (AvarioException | NumberFormatException exception) {
                brightness = 0;
            }

            stateTmp = brightness <= 0 ? "off" : "on";

            // consider the whole thing turned on if one of them is on
            if (state == null || stateTmp.equals("on"))
                state = stateTmp;

            if (state.equals("on"))
                break;
        }

        return state;
    }

    /**
     * Returns the "priority" state for the cover dial. Particularly useful on multiple entities.
     *
     * @return the state of the cover (Constants.ENTITY_COVER_STATE_*) or null when dial type is not cover
     */
    private String getStateCover() {
        if (this.type != Type.COVER)
            return null;

        String state = this.entities.isEmpty()
                ? Constants.ENTITY_COVER_STATE_CLOSED
                : null;

        for (JSONObject entityJSON : this.entities) {
            String tmp;

            try {
                tmp = entityJSON
                        .getJSONObject("new_state")
                        .getString("state");
            } catch (JSONException exception) {
                tmp = Constants.ENTITY_COVER_STATE_CLOSED;
            }

            if (state == null)
                state = tmp;
            else if (state.equals(Constants.ENTITY_COVER_STATE_OPENING))
                break;
            else if (state.equals(Constants.ENTITY_COVER_STATE_CLOSING)
                    && tmp.equals(Constants.ENTITY_COVER_STATE_OPENING)) {
                state = tmp;
                break;
            } else if (!state.equals(tmp)) {
                state = Constants.ENTITY_COVER_STATE_PARTIAL;
                break;
            }
        }

        return state;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            this.arc.setEnabled(true);
        } else {
            this.arc.setEnabled(false);
            this.arc.setValue(0, true);

            this.unrender();
            this.dialJSON = null;
        }
    }

    public void swapMediaDialType() throws AvarioException {
        JSONObject entityJSON = this.entities.get(0);
        String dialId;
        String dpadId;
        String seekId;

        if (this.category == Category.ENTITY || this.dialJSON == null)
            return;

        // dial effectively becomes media type dial
        this.category = Category.MEDIA;

        try {
            dpadId = entityJSON
                    .getJSONObject("dials")
                    .getJSONObject("dpad")
                    .getString("dial_type");
        } catch (JSONException exception) {
            String[] msgArgs = new String[]{String.format(
                    "%s.dials.dpad.dial_type",
                    entityJSON.optString("entity_id")
            )};

            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    msgArgs
            );
        }

        try {
            seekId = entityJSON
                    .getJSONObject("dials")
                    .getJSONObject("seek")
                    .getString("dial_type");
        } catch (JSONException exception) {
            String[] msgArgs = new String[]{String.format(
                    "%s.dials.seek.dial_type",
                    entityJSON.optString("entity_id")
            )};

            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    msgArgs
            );
        }

        dialId = this.dialJSON.optString("entity_id").equals(dpadId)
                ? seekId
                : dpadId;

        this.unrender();

        try {
            this.dialJSON = StateArray
                    .getInstance()
                    .getDial(dialId);
            this.type = Type.fromId(dialId);
        } catch (AvarioException exception) {
            return;
        }

        this.render();
    }


    public void changeDialType(int id) throws AvarioException {
        JSONObject entityJSON = this.entities.get(0);
        String dialId = "";
        String brightnessId;
        String colourId;
        String saturationId;
        String tempratureId;

        switch (id) {
            case R.id.dialbtn__brightness:
                try {
                    brightnessId = entityJSON
                            .getJSONObject("dials")
                            .getJSONObject("brightness")
                            .getString("dial_type");
                } catch (JSONException exception) {
                    String[] msgArgs = new String[]{String.format(
                            "%s.dials.brightness.dial_type",
                            entityJSON.optString("entity_id")
                    )};

                    throw new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY,
                            exception,
                            msgArgs
                    );
                }

                dialId = brightnessId;
                break;
            case R.id.dialbtn__colour:
                try {
                    colourId = entityJSON
                            .getJSONObject("dials")
                            .getJSONObject("colour")
                            .getString("dial_type");
                } catch (JSONException exception) {
                    String[] msgArgs = new String[]{String.format(
                            "%s.dials.colour.dial_type",
                            entityJSON.optString("entity_id")
                    )};

                    throw new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY,
                            exception,
                            msgArgs
                    );
                }

                dialId = colourId;
                break;
            case R.id.dialbtn__saturation:
                try {
                    saturationId = entityJSON
                            .getJSONObject("dials")
                            .getJSONObject("saturation")
                            .getString("dial_type");
                } catch (JSONException exception) {
                    String[] msgArgs = new String[]{String.format(
                            "%s.dials.saturation.dial_type",
                            entityJSON.optString("entity_id")
                    )};

                    throw new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY,
                            exception,
                            msgArgs
                    );
                }

                dialId = saturationId;
                break;
            case R.id.dialbtn__temprature:
                try {
                    tempratureId = entityJSON
                            .getJSONObject("dials")
                            .getJSONObject("temprature")
                            .getString("dial_type");
                } catch (JSONException exception) {
                    String[] msgArgs = new String[]{String.format(
                            "%s.dials.saturation.dial_type",
                            entityJSON.optString("entity_id")
                    )};

                    throw new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY,
                            exception,
                            msgArgs
                    );
                }

                dialId = tempratureId;
                break;
        }
        this.unrender();

        try {
            this.dialJSON = StateArray
                    .getInstance()
                    .getDial(dialId);
            this.type = Type.fromId(dialId);
        } catch (AvarioException exception) {
            return;
        }

        this.render();
    }

    public void setup(List<JSONObject> entities, Dial.Category category, boolean isFromMQTT) {
        String prevId = this.entitiesId;

        this.entities = entities;
        this.entitiesId = EntityUtil.compileIds(this.entities);

        if (!this.entitiesId.equals(prevId)) {
            this.expectsMQTT = false;
            this.entitiesChanged = true;
            this.entitiesUpdated = null;

            NagleTimers.invalidate(Dial.TIMER_ID);
            APITimers.invalidate(Dial.TIMER_ID);
            APITimers.unlock(Dial.TIMER_ID);
        }

        this.category = category;

        switch (category) {
            case ENTITY:
                this.adapt(isFromMQTT);
                break;

            case MEDIA:
                this.adaptMedia();
                break;

            case VOLUME:
                this.adaptVolume();
                break;
        }
    }

    public void setup(List<JSONObject> entities) {
        this.setup(entities, Category.ENTITY, false);
    }

    public void initialize() {
        this.computeValue(Dial.SOURCE_SETUP);
    }

    private void adapt(boolean isFromMQTT) {
        JSONObject entityJSON;
        StateArray states = StateArray.getInstance();
        String dialId = null;
        String type;
        String defaultDial = null;
        JSONObject dials = null;
        Log.d("Category", category.toString() + " " + isFromMQTT);

        for (JSONObject entity : this.entities) {

            if (entity.has("dials")) {
                try {
                    defaultDial = entity.getString("default_dial");
                    type = entity
                            .getJSONObject("dials")
                            .getJSONObject(defaultDial)
                            .getString("dial_type");

                    if (dialId == null)
                        dialId = type;

                    if (!dialId.equals(type)) {
                        dialId = null;
                        break;
                    }
                    if (entity.has("dials")) {
                        dials = entity.getJSONObject("dials");
                    }

                } catch (JSONException e) {
                    AvarioException exception = new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY, e,
                            new Object[]{
                                    String.format("%s.dial.dial_type", entity.optString("entity_id"))
                            }
                    );

                    PlatformUtil
                            .getErrorToast(this.getContext(), exception)
                            .show();
                }
            } else {


                try {
                    type = entity
                            .getJSONObject("dial")
                            .getString("dial_type");

                    if (dialId == null)
                        dialId = type;

                    if (!dialId.equals(type)) {
                        dialId = null;
                        break;
                    }

                } catch (JSONException e) {
                    AvarioException exception = new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY, e,
                            new Object[]{
                                    String.format("%s.dials.dial_type", entity.optString("entity_id"))
                            }
                    );

                    PlatformUtil
                            .getErrorToast(this.getContext(), exception)
                            .show();
                }
            }
        }

        // check if the dial still needs to adapt or stay put
        Log.d("Dial id", dialId);
        try {

            dialId = dialId == null ? Type.SWITCH.getId() : dialId;
            if (isFromMQTT)
                return;

        } catch (NullPointerException ignored) {
        }

        Log.d(TAG, "Adapting to 1: " + dialId);

        this.unrender();

        try {
            this.dialJSON = states.getDial(dialId);
            this.type = Type.fromId(dialId);
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();

            return;
        }

        this.render();
    }

    private void adaptMedia() {
        JSONObject entityJSON;
        String dialId,
                type;

        // resolve the dialId to show depending on the bootstrap directive
        entityJSON = this.entities.get(0);
        type = entityJSON.optString("default_dial", null);

        try {
            JSONObject dialspecJSON = entityJSON.getJSONObject("dials");

            if (type == null)
                dialId = Type.MEDIADPAD.getId();
            else {
                dialId = dialspecJSON
                        .getJSONObject(type)
                        .getString("dial_type");
            }
        } catch (JSONException e) {
            AvarioException exception = new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY, e,
                    new Object[]{
                            String.format("%s.dials.%s.dial_type",
                                    entityJSON.optString("entity_id"),
                                    type
                            )
                    }
            );

            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();

            dialId = null;
        }

        // check if the dial still needs to adapt or stay put
        try {
            String currentId = this.dialJSON == null ? null : this.dialJSON.optString("entity_id");

            if (!this.entitiesChanged || (currentId != null &&
                    (currentId.equals(Type.MEDIADPAD.getId()) ||
                            currentId.equals(Type.MEDIANS.getId()) ||
                            currentId.equals(Type.MEDIASEEK.getId()) ||
                            currentId.equals(Type.VOLUME.getId())))
                    )
                return;
        } catch (NullPointerException ignored) {
        }

        Log.d(TAG, "Adapting to: " + dialId);

        this.unrender();

        try {
            this.dialJSON = StateArray
                    .getInstance()
                    .getDial(dialId);
            this.type = Type.fromId(dialId);
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();

            return;
        }

        this.render();
    }

    private void adaptVolume() {
        JSONObject entityJSON = this.entities.get(0);
        String dialId;

        // get spec object from entity first
        try {
            JSONObject dialspecJSON = entityJSON
                    .getJSONObject("dials")
                    .getJSONObject("volume");

            dialId = dialspecJSON.getString("dial_type");
        } catch (JSONException e) {
            AvarioException exception = new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY, e,
                    new Object[]{
                            String.format("%s.dials.volume.dial_type")
                    }
            );

            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();

            dialId = null;
        }

        // check if we need to change or stay put
        try {
            if (this.dialJSON.optString("entity_id").equals(dialId))
                return;
        } catch (NullPointerException ignored) {
        }

        this.unrender();

        try {
            this.dialJSON = StateArray
                    .getInstance()
                    .getDial(dialId);
            this.type = Type.fromId(dialId);
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();

            return;
        }

        this.render();
    }

    // region Dial Unrendering
    private void unrender() {
        if (this.type == null)
            return;

        this.arc.setBackground(AssetUtil.toDrawable(this.getContext(), R.array.bg__dial__top__flat));

        switch (this.type) {
            case SWITCH:
                this.unrenderSwitch();
                break;

            case LIGHT:
                this.unrenderLight();
                break;

            case VOLUME:
                this.unrenderVolume();
                break;

            case THERMO:
                this.unrenderThermo();
                break;

            case COVER:
                this.unrenderCover();
                break;

            case MEDIASEEK:
                this.unrenderMediaSeek();
                break;

            case MEDIANS:
                this.unrenderMediaNoSeek();
                break;

            case MEDIADPAD:
                this.unrenderMediaDpad();
                break;
            case COLOUR:
                this.unrenderColour();
                break;
            case SATURATION:
                this.unrenderSaturation();
                break;
            case TEMPERATURE:
                this.unrenderTemprature();
                break;
        }
    }

    private void unrenderSwitch() {
        this.switchHolder.setVisibility(View.GONE);
    }

    private void unrenderLight() {
        this.lightHolder.setVisibility(View.GONE);
    }

    private void unrenderVolume() {
        this.volumeHolder.setVisibility(View.GONE);
    }

    private void unrenderThermo() {
        this.thermoHolder.setVisibility(View.GONE);
    }

    private void unrenderCover() {
        this.coverHolder.setVisibility(View.GONE);
    }

    private void unrenderMediaSeek() {
        this.mediaseekHolder.setVisibility(View.GONE);
    }

    private void unrenderMediaNoSeek() {
        this.arc.setEnabled(true);
        this.mediaseekHolder.setVisibility(View.GONE);
    }

    private void unrenderMediaDpad() {
        this.arc.setEnabled(true);
        this.mediadpadHolder.setVisibility(View.GONE);
    }

    private void unrenderColour() {
        this.colourHolder.setVisibility(View.GONE);
        arc.setStartAngle(30);
        arc.setSweepAngle(300);
    }


    private void unrenderSaturation() {
        this.saturationHolder.setVisibility(View.GONE);
        arc.setStartAngle(30);
        arc.setSweepAngle(300);
    }

    private void unrenderTemprature() {
        this.tempratureHolder.setVisibility(View.GONE);
        arc.setStartAngle(30);
        arc.setSweepAngle(300);
    }

    // endregion

    // region Dial Rendering
    private void render() {
        if (this.dialJSON == null)
            return;

        Log.d("Type ", this.type.toString());

        switch (this.type) {
            case SWITCH:
                this.renderSwitch();
                break;

            case LIGHT:
                this.renderLight();
                break;

            case VOLUME:
                this.renderVolume();
                break;

            case THERMO:
                this.renderThermo();
                break;

            case COVER:
                this.renderCover();
                break;

            case MEDIASEEK:
                this.renderMediaSeek();
                break;

            case MEDIANS:
                this.renderMediaNoSeek();
                break;

            case MEDIADPAD:
                this.renderMediaDpad();
                break;
            case COLOUR:
                this.renderColour();
                break;
            case SATURATION:
                this.renderSaturation();
                break;
            case TEMPERATURE:
                this.renderTemprature();
                break;
        }

        this.showIdleState();
    }

    private void renderSwitch() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering switch..");

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(1);
        /*this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__color1),
                context.getResources().getColor(R.color.dial__color2),
                context.getResources().getColor(R.color.dial__color3)
        );*/
        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        if (!entities.isEmpty()) {
            JSONObject dial = entities.get(0).optJSONObject("dial");
            if (dial != null) {
                arcColourStart = dial.optString("arc_colour_start");
                arcColourEnd = dial.optString("arc_colour_end");
                /*handled bootstrap colors*/
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));
            }
        }
        this.switchHolder.setVisibility(View.VISIBLE);
    }

    private void renderLight() {
        final Context context = this.getContext();

        Log.d(TAG, "Rendering light..");
        Log.d(TAG, "Entities: " + entitiesId);

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(100); // TODO confirm with Richard. previous code: this.dialJSON.optInt("dial_max", 100));
        arc.setStartAngle(30);
        arc.setSweepAngle(300);
        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        JSONObject dials = entities.get(0).optJSONObject("dials");
        JSONObject dial = entities.get(0).optJSONObject("dials");

        try {
            if (dials != null) {
                Log.d("With diaols: ", dials.toString());

                arcColourStart = dials.getJSONObject("brightness").getString("arc_colour_start");
                arcColourEnd = dials.getJSONObject("brightness").getString("arc_colour_end");
                //handled bootstrap colors
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));
            } else if (dial != null) {
                Log.d("Dial: ", dial.toString());

                arcColourStart = dial.getString("arc_colour_start");
                arcColourEnd = dial.getString("arc_colour_end");
                //handled bootstrap colors
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        lightHolder.setVisibility(View.VISIBLE);
    }

    private void renderColour() {
        final Context context = this.getContext();

        Log.d(TAG, "Rendering Hue..");
        Log.d(TAG, "Entities: " + entitiesId);

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(360);

        arc.setSeekColor(getResources().getColor(R.color.trasnparent));
        arc.setStartAngle(0);
        arc.setSweepAngle(360);
        arc.setValue(360, true);
        arc.setHue();
        arc.setProgress(360);
        colourHolder.setVisibility(View.VISIBLE);
    }


    private void renderSaturation() {
        final Context context = this.getContext();
        JSONArray rgbArray = null;
        int[] rgb = new int[3];

        Log.d(TAG, "Rendering light..");
        Log.d(TAG, "Entities: " + entitiesId);

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(255);
        try {
            rgbArray = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getJSONArray("rgb_color");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        arc.setSeekColor(getResources().getColor(R.color.trasnparent));
        arc.setStartAngle(30);
        arc.setSweepAngle(300);
        arc.setValue(300, true);
        if (rgbArray != null) {

            rgb = new int[rgbArray.length()];

            for (int i = 0; i < rgbArray.length(); i++) {
                rgb[i] = rgbArray.optInt(i);
            }

            float[] hsvColor = new float[3];
            Color.RGBToHSV(rgb[0], rgb[1], rgb[2], hsvColor);

            arc.setSaturation(hsvColor[0]);
            //arc.setProgressColor(Color.HSVToColor(hsvColor), getResources().getColor(R.color.white));
        } else {
            float[] hsvColor = new float[3];
            Color.RGBToHSV(255, 0, 0, hsvColor);
            arc.setSaturation(hsvColor[0]);
            //arc.setProgressColor(Color.HSVToColor(hsvColor), getResources().getColor(R.color.white));
        }

        arc.setProgress(255);
        saturationHolder.setVisibility(View.VISIBLE);
    }

    private void renderTemprature() {
        final Context context = this.getContext();
        JSONArray rgbArray = null;
        int[] rgb = new int[3];

        Log.d(TAG, "Rendering light..");
        Log.d(TAG, "Entities: " + entitiesId);

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(100); // TODO confirm with Richard. previous code: this.dialJSON.optInt("dial_max", 100));
        try {
            rgbArray = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getJSONArray("rgb_color");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        arc.setSeekColor(getResources().getColor(R.color.trasnparent));
        arc.setStartAngle(30);
        arc.setSweepAngle(300);
        arc.setValue(300, true);
        arc.setProgressColor(Color.parseColor("#A6D1FF"), Color.parseColor("#ffffff"), Color.parseColor("#FFA000"));
        arc.setProgress(100);
        tempratureHolder.setVisibility(View.VISIBLE);
    }

    private void renderVolume() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering volume..");

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(100); // TODO confirm with Richard. previous code: this.dialJSON.optInt("dial_max", 100));
       /* this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__color1),
                context.getResources().getColor(R.color.dial__color2),
                context.getResources().getColor(R.color.dial__color3)
        );*/

        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        JSONObject dial = entities.get(0).optJSONObject("dial");
        if (dial != null) {
            Log.d("Dial: ", dial.toString());

            arcColourStart = dial.optString("arc_colour_start");
            arcColourEnd = dial.optString("arc_colour_end");
            //handled bootstrap colors
            if (!arcColourStart.isEmpty())
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));

        }
        this.volumeHolder.setVisibility(View.VISIBLE);
    }

    private void renderThermo() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering thermo...");

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__button,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(100); // TODO confirm with Richard. previous code: this.dialJSON.optInt("dial_max", 100));
        /*this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__temperature1),
                context.getResources().getColor(R.color.dial__temperature2),
                context.getResources().getColor(R.color.dial__temperature3),
                context.getResources().getColor(R.color.dial__temperature4),
                context.getResources().getColor(R.color.dial__temperature5),
                context.getResources().getColor(R.color.dial__temperature6)
        );*/

        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        JSONObject dial = entities.get(0).optJSONObject("dial");
        if (dial != null) {
            Log.d("Dial: ", dial.toString());

            arcColourStart = dial.optString("arc_colour_start");
            arcColourEnd = dial.optString("arc_colour_end");
            //handled bootstrap colors
            if (!arcColourStart.isEmpty())
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));

        }
        this.thermoHolder.setVisibility(View.VISIBLE);
    }

    private void renderCover() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering cover...");

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__flat,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        this.arc.setMax(100); // TODO confirm with Richard. previous code: this.dialJSON.optInt("dial_max", 100));
        /*this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__color1),
                context.getResources().getColor(R.color.dial__color2),
                context.getResources().getColor(R.color.dial__color3)
        );*/
        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        JSONObject dial = entities.get(0).optJSONObject("dial");
        if (dial != null) {
            Log.d("Dial: ", dial.toString());

            arcColourStart = dial.optString("arc_colour_start");
            arcColourEnd = dial.optString("arc_colour_end");
            //handled bootstrap colors
            if (!arcColourStart.isEmpty())
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));

        }
        this.coverHolder.setVisibility(View.VISIBLE);
    }

    private void renderMediaSeek() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering media seek...");

        if (this.entities == null || this.entities.isEmpty())
            return;

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__flat,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        AssetUtil.toDrawable(
                context,
                R.array.ic__dial__dimple,
                new ArcThumbCallback(this.arc)
        );

        /*this.arc.setMax(this.getMediaDuration());
        this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__color1),
                context.getResources().getColor(R.color.dial__color2),
                context.getResources().getColor(R.color.dial__color3)
        );*/

        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        JSONObject dial = entities.get(0).optJSONObject("dial");
        if (dial != null) {
            Log.d("Dial: ", dial.toString());

            arcColourStart = dial.optString("arc_colour_start");
            arcColourEnd = dial.optString("arc_colour_end");
            //handled bootstrap colors
            if (!arcColourStart.isEmpty())
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));

        }
        this.mediaseekHolder.setVisibility(View.VISIBLE);
    }

    private void renderMediaNoSeek() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering media no seek...");

        if (this.entities == null || this.entities.isEmpty())
            return;

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__flat,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        this.arc.setEnabled(false);
        this.arc.setMax(0);
        this.arc.setValue(0, true);
        /*this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__color1),
                context.getResources().getColor(R.color.dial__color2),
                context.getResources().getColor(R.color.dial__color3)
        );*/

        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";
        JSONObject dial = entities.get(0).optJSONObject("dial");
        if (dial != null) {
            Log.d("Dial: ", dial.toString());

            arcColourStart = dial.optString("arc_colour_start");
            arcColourEnd = dial.optString("arc_colour_end");
            //handled bootstrap colors
            if (!arcColourStart.isEmpty())
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));

        }
        this.mediaseekHolder.setVisibility(View.VISIBLE);
    }

    private void renderMediaDpad() {
        Context context = this.getContext();

        Log.d(TAG, "Rendering media pad...");

        AssetUtil.toDrawable(
                context,
                R.array.bg__dial__top__dpad,
                new AssetUtil.BackgroundCallback(this.arc)
        );

        this.arc.setEnabled(false);
        this.arc.setMax(1);
        this.arc.setValue(0, true);
        this.mediadpadHolder.setVisibility(View.VISIBLE);

        /*this.arc.setProgressColor(
                context.getResources().getColor(R.color.dial__color1),
                context.getResources().getColor(R.color.dial__color2),
                context.getResources().getColor(R.color.dial__color3)
        );*/
        String arcColourStart = "";
        String arcColourMid = "";
        String arcColourEnd = "";

        JSONObject dial = entities.get(0).optJSONObject("dial");
        if (dial != null) {
            Log.d("Dial: ", dial.toString());

            arcColourStart = dial.optString("arc_colour_start");
            arcColourEnd = dial.optString("arc_colour_end");
            //handled bootstrap colors
            if (!arcColourStart.isEmpty())
                arc.setProgressColor(Color.parseColor(arcColourStart), Color.parseColor(arcColourEnd));
        }
    }
    // endregion

    // region UI Updates

    /**
     * Shows the necessary views when the dial is being interacted on. (e.g. for light dial, the
     * percentage will be shown)
     */
    private void showActiveState() {
        switch (this.type) {
            case LIGHT:
                this.lightPowerIB.setVisibility(View.GONE);
                this.lightPercentTV.setVisibility(View.VISIBLE);
                break;

            case VOLUME:
                this.volumePowerIB.setVisibility(View.GONE);
                this.volumePercentTV.setVisibility(View.VISIBLE);
                break;

            case THERMO:
                this.thermoPowerIB.setVisibility(View.GONE);
                this.thermoPercentTV.setVisibility(View.VISIBLE);
                break;

            case SWITCH:
                break;

            case COVER:
                this.coverCtrlHolder.setVisibility(View.GONE);
                this.coverPercentTV.setVisibility(View.VISIBLE);
                break;
            case COLOUR:
                this.colourPowerIB.setVisibility(View.GONE);
                this.colourPercentTV.setVisibility(View.VISIBLE);
                break;
            case SATURATION:
                this.saturationPowerIB.setVisibility(View.GONE);
                this.saturationPercentTV.setVisibility(View.VISIBLE);
                break;
            case TEMPERATURE:
                this.tempraturePowerIB.setVisibility(View.GONE);
                this.tempraturePercentTV.setVisibility(View.VISIBLE);
                break;
            default:
        }
    }

    /**
     * Shows the necessary views when the dial is idle. (e.g. for light dial, the power icon will
     * be shown)
     */
    private void showIdleState() {
        switch (this.type) {
            case LIGHT:
                this.lightPowerIB.setVisibility(View.VISIBLE);
                this.lightPercentTV.setVisibility(View.GONE);

            case VOLUME:
                this.volumePowerIB.setVisibility(View.VISIBLE);
                this.volumePercentTV.setVisibility(View.GONE);
                break;

            case THERMO:
                this.thermoPowerIB.setVisibility(View.VISIBLE);
                this.thermoPercentTV.setVisibility(View.GONE);
                break;

            case SWITCH:
                break;

            case COVER:
                this.coverCtrlHolder.setVisibility(View.VISIBLE);
                this.coverPercentTV.setVisibility(View.GONE);
                break;

            case COLOUR:
                this.colourPowerIB.setVisibility(View.VISIBLE);
                this.colourPercentTV.setVisibility(View.GONE);
                break;
            case SATURATION:
                this.saturationPowerIB.setVisibility(View.VISIBLE);
                this.saturationPercentTV.setVisibility(View.GONE);
                break;
            case TEMPERATURE:
                this.tempraturePowerIB.setVisibility(View.VISIBLE);
                this.tempraturePercentTV.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Updates the arc of the dial depending on the source of the value
     *
     * @param value  the value of the dial
     * @param source whether or not this is called from an MQTT update
     */
    private void updateDial(int value, int source, boolean updateThumb) {
        if (source == Dial.SOURCE_MQTT)
            if (this.entitiesUpdated == null
                    || this.entitiesUpdated.size() == this.entities.size()
                    || !this.expectsMQTT)
                this.arc.setValue(value, updateThumb);
            else
                this.arc.setProgress(value);
        else
            this.arc.setValue(value, updateThumb);
    }

    private void refreshControls(int value, boolean fromUser) {
        String units = this.dialJSON.optString("units", "");

        switch (this.type) {
            case SWITCH:
                this.refreshSwitch(value);
                break;

            case LIGHT:
                this.refreshLight(value, fromUser, units);
                break;

            case VOLUME:
                this.refreshVolume(value, fromUser, units);
                break;

            case THERMO:
                this.refreshThermo(value, fromUser, units);
                break;

            case COVER:
                this.refreshCover(value, units);
                break;
            case COLOUR:
                this.refreshColour(value, fromUser, units);
                break;
            case SATURATION:
                this.refreshSaturation(value, fromUser, units);
                break;
            case TEMPERATURE:
                this.refreshTemprature(value, fromUser, units);
                break;
            case MEDIASEEK:
            case MEDIANS:
                this.refreshMediaSeek(value);
                break;
        }
    }

    private void refreshSwitch(int value) {
        this.switchPowerIB.setActivated(value > 0);
    }

    private void refreshLight(int value, boolean fromUser, String units) {
        if (fromUser)
            this.progressPrev = value;

        this.lightPowerIB.setActivated(value > 0);
        this.lightPercentTV.setText(this.getResources().getString(
                R.string.dial__powervalue,
                value,
                units
        ));
    }

    private void refreshColour(int value, boolean fromUser, String units) {
        if (fromUser)
            this.progressPrev = value;

        this.colourPowerIB.setActivated(true);
        this.colourPercentTV.setText(this.getResources().getString(
                R.string.dial__powervalue,
                value,
                units
        ));
    }

    private void refreshSaturation(int value, boolean fromUser, String units) {
        if (fromUser)
            this.progressPrev = value;

        this.saturationPowerIB.setActivated(true);
        this.saturationPercentTV.setText(value + "");
    }

    private void refreshTemprature(int value, boolean fromUser, String units) {
        if (fromUser)
            this.progressPrev = value;
        int temperature = ((value * 350) / 100) + 150;
        this.tempraturePowerIB.setActivated(true);
        this.tempraturePercentTV.setText(temperature + "");
        Log.d("temprature inside", value + " " + value);
    }

    private void refreshThermo(int value, boolean fromUser, String units) {
        if (fromUser)
            this.progressPrev = value;


        this.thermoPowerIB.setActivated(value > 0);
        this.thermoPercentTV.setText(this.getResources().getString(
                R.string.dial__powervalue,
                value,
                units
        ));
    }

    private void refreshVolume(int value, boolean fromUser, String units) {
        if (fromUser)
            this.progressPrev = value;

        String state;

        try {
            state = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getString("state");
        } catch (JSONException | IndexOutOfBoundsException exception) {
            return; // do nothing. nothing to update
        }

        this.volumePowerIB.setActivated(state.equals(Constants.ENTITY_MEDIA_STATE_PLAYING));
        this.volumePercentTV.setText(this.getResources().getString(
                R.string.dial__powervalue,
                value,
                units
        ));
    }

    private void refreshCover(int value, String units) {
        Resources res = this.getResources();

        int selectedClr = res.getColor(android.R.color.white),
                defaultClr = res.getColor(R.color.gray2);

        // find the "priority" state.
        // so it's "opening" > "closing" > "partial" > "open" > "closed"

        String state = this.getStateCover();
        int assetId;

        switch (state) {
            case Constants.ENTITY_COVER_STATE_OPENED:
            case Constants.ENTITY_COVER_STATE_OPENING:
                this.coverOpenTV.setTextColor(selectedClr);
                this.coverCloseTV.setTextColor(defaultClr);
                assetId = R.array.bg__dial__mid__open;
                break;

            case Constants.ENTITY_COVER_STATE_CLOSED:
            case Constants.ENTITY_COVER_STATE_CLOSING:
                this.coverOpenTV.setTextColor(defaultClr);
                this.coverCloseTV.setTextColor(selectedClr);
                assetId = R.array.bg__dial__mid__close;
                break;

            default: // partial
                this.coverOpenTV.setTextColor(defaultClr);
                this.coverCloseTV.setTextColor(defaultClr);
                assetId = R.array.bg__dial__mid__unselected;
        }

        AssetUtil.toDrawable(
                this.getContext(),
                assetId,
                new AssetUtil.BackgroundCallback(this.coverCtrlHolder)
        );

        this.coverPercentTV.setText(value + units);

        if (state.equals(Constants.ENTITY_COVER_STATE_OPENING)) {
            this.coverOpenTV.setText(R.string.dial__coverstop);
            this.coverCloseTV.setText(R.string.dial__coverclose);
            this.coverOpenTV.setTag(R.id.tag__cover__stop, true);
            this.coverCloseTV.setTag(R.id.tag__cover__stop, null);
        } else if (state.equals(Constants.ENTITY_COVER_STATE_CLOSING)) {
            this.coverOpenTV.setText(R.string.dial__coveropen);
            this.coverCloseTV.setText(R.string.dial__coverstop);
            this.coverOpenTV.setTag(R.id.tag__cover__stop, null);
            this.coverCloseTV.setTag(R.id.tag__cover__stop, true);
        } else {
            this.coverOpenTV.setText(R.string.dial__coveropen);
            this.coverCloseTV.setText(R.string.dial__coverclose);
            this.coverOpenTV.setTag(R.id.tag__cover__stop, null);
            this.coverCloseTV.setTag(R.id.tag__cover__stop, null);
        }
    }

    private void refreshMediaSeek(int value) {
        int duration = this.getMediaDuration();

        if (value >= 0)
            this.mediaseekCurrentTV.setText(PlatformUtil.toTimeString(value));
        else
            this.mediaseekCurrentTV.setText(R.string.medialist__unknown__timer);

        if (duration >= 0)
            this.mediaseekRemainTV.setText(PlatformUtil.toTimeString(this.getMediaDuration()));
        else
            this.mediaseekRemainTV.setText(R.string.medialist__unknown__timer);
    }

    // endregion

    // region Value Computation
    private void computeValue(int source) {
        switch (this.type) {
            case SWITCH:
                this.computeValueSwitch(source);
                break;

            case LIGHT:
                this.computeValueLight(source);
                break;

            case VOLUME:
                this.computeValueVolume(source);
                break;

            case THERMO:
                this.computeValueThermo(source);
                break;

            case COVER:
                this.computeValueCover(source);
                break;

            case MEDIASEEK:
                this.computeValueMediaSeek(source);
                break;

            case MEDIANS:
                this.computeValueMediaNoSeek(source);
                break;
            case COLOUR:
                this.computeValueColour(source);
                break;
            case SATURATION:
                this.computeValueSaturation(source);
                break;
            case TEMPERATURE:
                this.computeValueTemprature(source);
                break;
        }
    }

    private void computeValueSwitch(int source) {
        String state = this.getStateSwitch();
        int valueArc = state == null || state.equals("off") ? 0 : 1;

        if (source == Dial.SOURCE_USER)
            this.progressPrev = valueArc;
        Log.d("Switch", valueArc + " " + state.equals("off"));
        this.updateDial(valueArc, source, true);
        this.refreshControls(valueArc, false);
    }

    private void computeValueLight(int source) {
        this.computeValuePower("light_algo", source);
    }

    private void computeValueVolume(int source) {
        int volume;

        try {
            volume = (int) Math.round(Constants.MAX_VALUE_VOLUME * this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getDouble("volume_level"));
        } catch (JSONException | IndexOutOfBoundsException exception) {
            return;
        }

        this.updateDial(volume, source, true);
        this.refreshControls(volume, false);
    }

    private void computeValueColour(int source) {
        JSONArray value;
        int[] rgb;
        float[] hsv = new float[3];
        try {
            value = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getJSONArray("rgb_color");
        } catch (JSONException | IndexOutOfBoundsException exception) {
            return;
        }

        rgb = new int[value.length()];

        for (int i = 0; i < value.length(); i++) {
            rgb[i] = value.optInt(i);
        }

        Color.RGBToHSV(rgb[0], rgb[1], rgb[2], hsv);

        int hue = (int) hsv[0];
        Log.d("Hue", hue + "");
        this.updateDial(360, source, false);
        this.arc.setThumbPosition(hue);
        this.refreshControls(hue, false);
    }

    private void computeValueSaturation(int source) {
        int value;

        try {
            value = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getInt("white_value");
        } catch (JSONException | IndexOutOfBoundsException exception) {
            return;
        }

        int percentage = (value * 100) / 255;
        Log.d("percentage", percentage + " " + value);
        this.updateDial(255, source, false);
        this.arc.updateThumbPositionCustom(value);
        this.refreshControls(value, false);
    }

    private void computeValueTemprature(int source) {
        int value;
        try {
            value = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getInt("color_temp");
        } catch (JSONException | IndexOutOfBoundsException exception) {
            return;
        }

        int percentage = ((value - 150) * 100) / 350;
        this.updateDial(100, source, false);
        this.arc.updateThumbPositionCustom(percentage);
        this.refreshControls(percentage, false);
    }

    private void computeValueThermo(int source) {
        this.computeValuePower("thermo_algo", source);
    }

    /**
     * computes value for power-button based dials. Namely currently, these are LIGHT and THERMO
     * type dials
     *
     * @param source
     * @oaran algoEntityId
     */
    private void computeValuePower(String algoEntityId, int source) {
        List<Float> values = new ArrayList<>();
        float value;
        int valueArc;

        for (JSONObject entityJSON : this.entities) {
            try {
                values.add(Float.parseFloat(RefStringUtil.processCode(
                        entityJSON.optString("value")
                )));
            } catch (AvarioException | NumberFormatException exception) {
                values.add(0f);
            }
        }

        String algorithm;

        try {
            JSONObject algoJSON = StateArray
                    .getInstance()
                    .getDialButtonState(algoEntityId);

            try {
                algorithm = algoJSON
                        .getJSONObject("states")
                        .getString(PlatformUtil.hashMD5(this.entitiesId));
            } catch (JSONException exception) {
                throw new AvarioException(
                        Constants.ERROR_STATE_MISSINGKEY,
                        exception,
                        new Object[]{algoEntityId + ".states." + PlatformUtil.hashMD5(this.entitiesId)}
                );
            }
        } catch (AvarioException exception) {
            algorithm = "aligned";
        }

        switch (algorithm) {
            case "aligned":
                value = this.applyFormulaAligned(values);
                break;

            case "converge":
                value = this.applyFormulaConverge(
                        Collections.min(values),
                        Collections.max(values)
                );
                break;

            case "relative":
                value = this.applyFormulaRelative(
                        Collections.min(values),
                        Collections.max(values)
                );
                break;

            default:
                value = 50f;
        }

        value = value / Constants.MAX_VALUE_NUMBER * 100.00f;
        valueArc = Math.round(value);

        if (source == Dial.SOURCE_USER)
            this.progressPrev = valueArc;

        this.updateDial(valueArc, source, true);
        this.refreshControls(valueArc, false);
    }

    private void computeValueCover(int source) {
        int valueArc;

        if (this.entities == null || this.entities.isEmpty())
            valueArc = 50;
        else {
            valueArc = 0;

            for (JSONObject entityJSON : this.entities) {
                int item;

                try {
                    item = (int) Math.round(
                            entityJSON
                                    .getJSONObject("new_state")
                                    .getJSONObject("attributes")
                                    .getDouble("current_position")
                    );
                } catch (JSONException | NumberFormatException exception) {
                    item = 0;
                }

                valueArc += item;
            }

            valueArc = valueArc / this.entities.size();
        }

        this.updateDial(valueArc, source, true);
        this.refreshControls(valueArc, false);
    }

    private void computeValueMediaSeek(int source) {
        JSONObject entityJSON;
        int valueArc,
                valueCtrl;

        try {
            entityJSON = this.entities.get(0);
            valueArc = EntityUtil.getSeekPosition(entityJSON);
            valueCtrl = valueArc;
        } catch (IndexOutOfBoundsException | AvarioException exception) {
            valueArc = 0;
            valueCtrl = -1;
        }

        this.arc.setMax(this.getMediaDuration());

        this.updateDial(valueArc, source, true);
        this.refreshControls(valueCtrl, false);
    }

    private void computeValueMediaNoSeek(int source) {
        JSONObject entityJSON;
        int valueCtrl;

        try {
            entityJSON = this.entities.get(0);
            valueCtrl = EntityUtil.getSeekPosition(entityJSON);
        } catch (IndexOutOfBoundsException | AvarioException exception) {
            valueCtrl = -1;
        }

        this.refreshControls(valueCtrl, false);
    }
    // endregion

    // region Formulas
    private float applyFormulaConverge(float min, float max) {
        return max * 100.00f / (100.00f + max - min);
    }

    private float applyFormulaRelative(float min, float max) {
        return this.applyFormulaConverge(min, max);
    }

    private float applyFormulaAligned(List<Float> values) {
        float total = 0;

        for (float value : values)
            total += value;

        return total / values.size();
    }
    // endregion

    // region API Execution
    private JSONObject getRequestSpec(String state) throws AvarioException {
        Log.d("state ", state);
        Log.d("json", this.dialJSON.toString());
        try {
            return new JSONObject(
                    this.dialJSON
                            .getJSONObject("controls")
                            .getJSONObject(state)
                            .toString()
            );
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{String.format("%s.controls.%s", this.dialJSON.optString("entity_id"), state)}
            );
        }
    }

    private void processRequestSpec(JSONObject specJSON, String value, String algo) throws AvarioException {
        if (!APIClient.isValidRequestSpec(specJSON))
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    null,
                    new Object[]{this.dialJSON.optString("entity_id"), 0}
            );

        try {
            // payload processing
            Matcher matcher = RefStringUtil.extractMarkers(specJSON.getString("payload"), null);
            Map<String, String> mapping;

            mapping = new HashMap<>();
            mapping.put("entity_ids", this.entitiesId);
            mapping.put("value", value);
            mapping.put("algo", algo);

            specJSON.put("payload", RefStringUtil.replaceMarkers(matcher, mapping));
        } catch (JSONException ignored) {
        }
    }

    private void processRequestSpec(JSONObject specJSON, int[] value) throws AvarioException {
        if (!APIClient.isValidRequestSpec(specJSON))
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    null,
                    new Object[]{this.dialJSON.optString("entity_id"), 0}
            );

        try {
            // payload processing
            Matcher matcher = RefStringUtil.extractMarkers(specJSON.getString("payload"), null);
            Map<String, Object> mapping;

            mapping = new HashMap<>();
            mapping.put("entity_ids", this.entitiesId);
            mapping.put("rgb_color", value);


            Gson gson = new Gson();
            ColorBody colorBody = new ColorBody();
            colorBody.setEntityId(this.entitiesId);
            colorBody.setRgbColor(value);

            String jsonBody = gson.toJson(colorBody);

            specJSON.put("payload", jsonBody);
        } catch (JSONException ignored) {
        }
    }

    private void processRequestSpecTemperature(JSONObject specJSON, int value) throws AvarioException {
        if (!APIClient.isValidRequestSpec(specJSON))
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    null,
                    new Object[]{this.dialJSON.optString("entity_id"), 0}
            );

        try {
            // payload processing
            Matcher matcher = RefStringUtil.extractMarkers(specJSON.getString("payload"), null);
            Map<String, Object> mapping;

            mapping = new HashMap<>();
            mapping.put("entity_ids", this.entitiesId);
            mapping.put("color_temp", value);


            Gson gson = new Gson();
            TemperatureBody temperatureBody = new TemperatureBody();
            temperatureBody.setEntityId(this.entitiesId);
            temperatureBody.setColorTemp(value);

            String jsonBody = gson.toJson(temperatureBody);

            specJSON.put("payload", jsonBody);
        } catch (JSONException ignored) {
        }
    }

    private void processRequestSpec(JSONObject specJSON, int value) throws AvarioException {
        if (!APIClient.isValidRequestSpec(specJSON))
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    null,
                    new Object[]{this.dialJSON.optString("entity_id"), 0}
            );

        try {
            // payload processing
            Matcher matcher = RefStringUtil.extractMarkers(specJSON.getString("payload"), null);
            Map<String, Object> mapping;

            mapping = new HashMap<>();
            mapping.put("entity_ids", this.entitiesId);
            mapping.put("rgb_color", value);


            Gson gson = new Gson();
            SaturationBody saturationBody = new SaturationBody();
            saturationBody.setEntityId(this.entitiesId);
            saturationBody.setWhiteValue(value);

            String jsonBody = gson.toJson(saturationBody);

            specJSON.put("payload", jsonBody);
        } catch (JSONException ignored) {
        }
    }

    private void executeAPI(View source) {
        JSONObject specJSON;
        Log.d("Type api", this.type.toString());
        try {
            switch (this.type) {
                case SWITCH:
                    specJSON = this.executeAPISwitch(source);
                    break;

                case LIGHT:
                    specJSON = this.executeAPILight(source);
                    break;

                case VOLUME:
                    specJSON = this.executeAPIVolume(source);
                    break;

                case THERMO:
                    specJSON = this.executeAPIThermo(source);
                    break;

                case COVER:
                    specJSON = this.executeAPICover(source);
                    break;

                case MEDIASEEK:
                    specJSON = this.executeAPIMediaSeek();
                    break;

                case MEDIADPAD:
                    specJSON = this.executeAPIMediaDpad(source);
                    break;
                case COLOUR:
                    specJSON = this.executeAPIColor(source);
                    break;
                case SATURATION:
                    specJSON = this.executeAPISaturation(source);
                    break;
                case TEMPERATURE:
                    specJSON = this.executeAPITemperature(source);
                    break;
                default:
                    specJSON = null;
            }

            if (specJSON == null) {
                Application.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Dial.this.computeValue(Dial.SOURCE_TIME);
                    }
                });

                return;
            }

            this.entitiesUpdated = null;
            this.expectsMQTT = true;

            APIClient.getInstance().executeRequest(
                    specJSON,
                    this.dialJSON.optString("entity_id"),
                    Dial.TIMER_ID,
                    new APIListener()
            );
        } catch (final AvarioException exception) {
            Application.mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    PlatformUtil
                            .getErrorToast(Dial.this.getContext(), exception)
                            .show();
                }
            });
        }
    }

    private JSONObject executeAPILight(View source) throws AvarioException {
        String directive;
        int progress;

        progress = this.arc.getValue();
        directive = this.arc == source
                ? "set"
                : progress > 0 ? "on" : "off";

        if (!directive.equals("set") && directive.equals(this.getStateLight()))
            return null;

        JSONObject specJSON = this.getRequestSpec(
                this.arc == source ? "set" :
                        progress > 0 ? "on" : "off"
        );
        Log.d("specJson: ", specJSON.toString());
        if (this.arc != source) {
            isLightsOn = !(progress > 0);
        } else {
            isLightsOn = true;
        }

        // TODO: 10/17/17 this is where brightness gets calculated John notes
        this.processRequestSpec(
                specJSON,
                String.valueOf(Math.round(progress * Constants.MAX_VALUE_NUMBER / 100f)),
                Light.getInstance().currentAlgo
        );
        return specJSON;
    }

    private JSONObject executeAPIColor(View source) throws AvarioException {
        String directive;
        int progress = 0;

        directive = this.arc == source
                ? "set"
                : getStateLight().equals("on") ? "off" : "on";

        Log.d("State light: ", this.getStateLight());
        if (!directive.equals("set") && directive.equals(this.getStateLight()))
            return null;

        JSONObject specJSON = this.getRequestSpec(
                this.arc == source ? "set" :
                        getStateLight().equals("on") ? "off" : "on"
        );
        //for coloring
        int[] rgbint = new int[3];
        progress = this.arc.getValue();
        int colorInt = Color.HSVToColor(new float[]{progress, 1, 1});
        color = colorInt;
        int red = Color.red(colorInt);
        int green = Color.green(colorInt);
        int blue = Color.blue(colorInt);

        rgbint[0] = red;
        rgbint[1] = green;
        rgbint[2] = blue;
        Log.d("specJson: ", specJSON.toString());

        if (getStateLight().equals("on")) {
            arc.setThumbDrawable(null);
        }

        if (this.arc == source) {
            this.processRequestSpec(specJSON, rgbint);
        } else {
            this.processRequestSpec(
                    specJSON,
                    String.valueOf(Math.round(progress * Constants.MAX_VALUE_NUMBER / 100f)),
                    Light.getInstance().currentAlgo
            );
        }

        return specJSON;
    }

    private JSONObject executeAPISaturation(View source) throws AvarioException {
        String directive;
        int progress = 0;
        int[] rgb;
        JSONArray rgbArray = null;

        directive = this.arc == source
                ? "set"
                : getStateLight().equals("on") ? "off" : "on";

        Log.d("State light: ", this.getStateLight());
        if (!directive.equals("set") && directive.equals(this.getStateLight()))
            return null;

        JSONObject specJSON = this.getRequestSpec(
                this.arc == source ? "set" :
                        getStateLight().equals("on") ? "off" : "on"
        );
        Log.d("Action: ", this.arc == source ? "set" :
                getStateLight().equals("on") ? "off" : "on");
        Log.d("entities", entities + " ");
        try {
            rgbArray = this.entities.get(0)
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getJSONArray("rgb_color");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (rgbArray != null) {

            rgb = new int[rgbArray.length()];

            for (int i = 0; i < rgbArray.length(); i++) {
                rgb[i] = rgbArray.optInt(i);
            }

            float[] hsvColor = new float[3];


            Color.RGBToHSV(rgb[0], rgb[1], rgb[2], hsvColor);
        } else {

            float[] hsvColor = new float[3];
            Color.RGBToHSV(255, 0, 0, hsvColor);
        }

        progress = this.arc.getValue();
        //int value = (255 * progress) / 100;
        if (getStateLight().equals("on")) {
            arc.setThumbDrawable(null);
        }

        Log.d("value", progress + "");

        if (this.arc == source) {
            this.processRequestSpec(specJSON, progress);
        } else {
            this.processRequestSpec(
                    specJSON,
                    String.valueOf(Math.round(progress * Constants.MAX_VALUE_NUMBER / 100f)),
                    Light.getInstance().currentAlgo
            );
        }

        return specJSON;
    }

    private JSONObject executeAPITemperature(View source) throws AvarioException {
        String directive;
        int progress = 0;

        directive = this.arc == source
                ? "set"
                : getStateLight().equals("on") ? "off" : "on";

        Log.d("State light: ", this.getStateLight());
        if (!directive.equals("set") && directive.equals(this.getStateLight()))
            return null;

        JSONObject specJSON = this.getRequestSpec(
                this.arc == source ? "set" :
                        getStateLight().equals("on") ? "off" : "on"
        );
        progress = this.arc.getValue();

        //val = ((percent * (max - min) / 100) + min
        int temperature = ((progress * 350) / 100) + 150;
        Log.d("Progress", progress + "");
        if (this.arc == source) {
            this.processRequestSpecTemperature(specJSON, temperature);
        } else {
            this.processRequestSpec(
                    specJSON,
                    String.valueOf(Math.round(progress * Constants.MAX_VALUE_NUMBER / 100f)),
                    Light.getInstance().currentAlgo
            );
        }

        return specJSON;
    }

    private JSONObject executeAPIVolume(View source) throws AvarioException {
        JSONObject specJSON;
        String directive;
        int progress;

        progress = this.arc.getValue();
        directive = this.arc == source ? "set"
                : progress > 0 ? "on"
                : "off";

        specJSON = this.getRequestSpec(directive);

        this.processRequestSpec(
                specJSON,
                String.format(Locale.US, "%.2f", progress / Constants.MAX_VALUE_VOLUME),
                ""
        );

        return specJSON;
    }

    private JSONObject executeAPIThermo(View source) throws AvarioException {
        return this.executeAPILight(source);
    }

    private JSONObject executeAPISwitch(View source) throws AvarioException {
        String directive;
        int value;

        value = this.arc.getValue() > 0 ? (int) Constants.MAX_VALUE_NUMBER : 0;
        directive = value > 0 ? "on" : "off";

        if (directive.equals(this.getStateSwitch()))
            return null;

        JSONObject specJSON = this.getRequestSpec(directive);

        this.processRequestSpec(
                specJSON,
                String.valueOf(value),
                ""
        );

        return specJSON;
    }

    private JSONObject executeAPICover(View source) throws AvarioException {
        String directive;
        boolean stop;

        try {
            stop = (Boolean) source.getTag(R.id.tag__cover__stop);
        } catch (NullPointerException exception) {
            stop = false;
        }

        directive = this.arc == source ? "set"
                : stop ? "stop"
                : source.getId() == R.id.cover__open ? "open" : "close";

        if ((!directive.equals("set") || !directive.equals("stop")) &&
                directive.equals(this.getStateCover()))
            return null;

        JSONObject specJSON = this.getRequestSpec(directive);
        int progress = this.arc.getValue();

        this.processRequestSpec(
                specJSON,
                String.valueOf(Math.round(progress * Constants.MAX_VALUE_COVER / 100f)),
                ""
        );

        return specJSON;
    }

    private JSONObject executeAPIMediaSeek() throws AvarioException {
        JSONObject specJSON = this.getRequestSpec("set");

        this.processRequestSpec(specJSON, String.valueOf(this.arc.getValue()), "");

        return specJSON;


    }

    private JSONObject executeAPIMediaDpad(View source) throws AvarioException {
        int sourceId = source.getId();

        String directive;
        JSONObject entityJSON,
                specJSON;

        try {
            entityJSON = this.entities.get(0);
        } catch (IndexOutOfBoundsException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new String[]{
                            String.format("dial entity @ index 0")
                    }
            );
        }

        directive = sourceId == R.id.dpad__up ? "up"
                : sourceId == R.id.dpad__down ? "down"
                : sourceId == R.id.dpad__left ? "left"
                : sourceId == R.id.dpad__right ? "right"
                : "select";

        try {
            specJSON = new JSONObject(
                    entityJSON
                            .getJSONObject("controls")
                            .getJSONObject(directive)
                            .toString()
            );
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new String[]{
                            String.format("%s.controls.%s", entityJSON.optString("entity_id"), directive)
                    }
            );
        }

        this.processRequestSpec(specJSON, "", "");

        return specJSON;
    }
    // endregion

    /*
     ***********************************************************************************************
     * Inner Classes
     ***********************************************************************************************
     */
    private class IdleRunnable implements Runnable {
        @Override
        public void run() {
            Dial.this.showIdleState();
        }
    }

    private class NagleRunnable implements Runnable {
        protected View source;

        private NagleRunnable(View source) {
            this.source = source;
        }

        @Override
        public void run() {
            Dial.this.executeAPI(this.source);
        }
    }

    /**
     * Implements a hold-and-repeat behaviour. Currently this is only particularly used for the
     * media dpad buttons
     */
    private class HoldRunnable extends NagleRunnable {
        private boolean repeat;

        private HoldRunnable() {
            super(null);
            this.repeat = true;
        }

        public HoldRunnable setSource(View source) {
            this.source = source;
            return this;
        }

        public HoldRunnable setRepeat(boolean repeat) {
            this.repeat = repeat;
            return this;
        }

        @Override
        public void run() {
            super.run();

            if (this.repeat)
                NagleTimers.reset(
                        Dial.TIMER_ID,
                        this,
                        Dial.this.getNagleDelay()
                );
            else
                NagleTimers.invalidate(Dial.TIMER_ID);
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
            Dial self = Dial.this;

            if (self.dialJSON == null || self.entities == null)
                return;

            StateArray states = StateArray.getInstance();
            String updatedId = intent.getStringExtra("entity_id");
            int from = intent.getIntExtra("from", StateArray.FROM_MQTT);

            if (updatedId != null)
                self.entitiesUpdated = self.entitiesUpdated != null
                        ? self.entitiesUpdated
                        : new ArrayList<String>();
            else
                self.entitiesUpdated = null;


            List<JSONObject> entities = new ArrayList<>();

            for (JSONObject entityJSON : self.entities) {
                String entityId = entityJSON.optString("entity_id");

                if (updatedId != null && entityId.equals(updatedId) && self.entitiesUpdated.indexOf(updatedId) == -1)
                    self.entitiesUpdated.add(updatedId);

                try {
                    // entity was updated in MQTT OR no specific devices was updated (hence all devices)
                    // refetch from StateArray
                    if (updatedId == null || entityId.equals(updatedId))
                        entities.add(self.category == Category.ENTITY
                                ? states.getEntity(entityId)
                                : states.getMediaEntity(entityId));
                        // entity is not updated
                    else
                        entities.add(entityJSON);
                } catch (AvarioException ignored) {
                }
            }

            // when all devices has had received their first MQTT update, kill timer
            if (self.expectsMQTT &&
                    (self.entitiesUpdated == null || self.entitiesUpdated.size() == self.entities.size())) {
                APITimers.unlock(Dial.TIMER_ID);
                APITimers.invalidate(Dial.TIMER_ID);
            }

            // translate StateArray.FROM_* to Dial.SOURCE_*
            // possible values of StateArray.FROM_* are:
            //      StateArray.FROM_MQTT
            //      StateArray.FROM_TIMER
            from = from == StateArray.FROM_MQTT
                    ? Dial.SOURCE_MQTT
                    : Dial.SOURCE_TIME;

            // conditions ("IT" being setup -> computeValue):
            // updatedId == null? DO IT
            // from MQTT and mqttId != null and entitiesId != null && entitiesId.contains(updatedId) ? DO IT
            if (updatedId == null
                    || self.entitiesId != null && self.entitiesId.contains(updatedId)) {
                self.setup(entities, self.category, true);
                self.computeValue(from);
            }
        }
    }

    private class TickerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Dial self = Dial.this;

            if (self.type != Type.MEDIASEEK)
                return;

            self.computeValue(Dial.SOURCE_TIME);
        }
    }


    private class APIListener extends APIRequestListener<String> {
        public APIListener() {
            super(Dial.TIMER_ID, Dial.this.entitiesId.split(","));
        }
    }


    private class SeekArcChangeListener implements SeekArc.OnSeekArcChangeListener {
        @Override
        public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
            Dial self = Dial.this;

            if (fromUser)
                NagleTimers.reset(Dial.TIMER_ID, new NagleRunnable(seekArc), self.getNagleDelay());

            self.mainHandler.removeCallbacks(self.idleRunnable);
            self.refreshControls(progress, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekArc seekArc) {
            Dial.this.showActiveState();
        }

        @Override
        public void onStopTrackingTouch(SeekArc seekArc) {
            Dial self = Dial.this;
            self.mainHandler.postDelayed(self.idleRunnable, 1000);
        }
    }

    private class AccessoryTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Dial self = Dial.this;
            int assetId = R.array.bg__dial__top__dpad;

            if (self.type != Type.MEDIADPAD)
                return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    switch (view.getId()) {
                        case R.id.dpad__center:
                            assetId = R.array.bg__dpad__middle__selected;
                            break;
                        case R.id.dpad__up:
                            assetId = R.array.bg__dpad__top__selected;
                            break;
                        case R.id.dpad__down:
                            assetId = R.array.bg__dpad__bottom__selected;
                            break;
                        case R.id.dpad__left:
                            assetId = R.array.bg__dpad__left__selected;
                            break;
                        case R.id.dpad__right:
                            assetId = R.array.bg__dpad__right__selected;
                            break;
                    }

                    self.holdRunnable
                            .setSource(view)
                            .setRepeat(true);

                    NagleTimers.reset(
                            Dial.TIMER_ID,
                            self.holdRunnable,
                            self.getNagleDelay()
                    );

                    break;

                case MotionEvent.ACTION_UP:
                    self.holdRunnable.setRepeat(false);
                    break;
            }

            AssetUtil.toDrawable(
                    self.getContext(),
                    assetId,
                    new AssetUtil.BackgroundCallback(self.arc)
            );

            return false;
        }
    }

    private class AccessoryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Dial self = Dial.this;
            boolean proceed = true;

            switch (self.type) {
                case SWITCH:
                    this.handleSwitch();
                    break;

                case LIGHT:
                    this.handleLight();
                    break;

                case THERMO:
                    this.handleThermo();
                    break;

                case COVER:
                    proceed = this.handleCover(view);
                    break;
                case COLOUR:
                    this.handleColour();
                    break;
                case SATURATION:
                    this.handleLight();
                    break;
                case TEMPERATURE:
                    this.handleLight();
                    break;
            }

            if (!proceed)
                return;

            self.refreshControls(self.arc.getValue(), false);
            NagleTimers.reset(Dial.TIMER_ID, new NagleRunnable(view), self.getNagleDelay());
        }

        private void handleSwitch() {
            Dial self = Dial.this;
            int value = self.arc.getValue() == 0 ? self.arc.getMax() : 0;

            self.arc.setDelta(value);
        }

        private void handleLight() {
            Dial self = Dial.this;
            int value = self.arc.getValue() == 0 ? self.progressPrev : 0;

            self.arc.setDelta(value);
        }

        private void handleColour() {
            Dial self = Dial.this;
            int value = self.arc.getValue() == 0 ? self.progressPrev : 0;

            //self.arc.setDelta(value);
        }

        private void handleThermo() {
            this.handleLight();
        }

        private boolean handleCover(View view) {
            Dial self = Dial.this;
            View other;
            int value;
            boolean stop,
                    handle;

            if (view == self.coverOpenTV) {
                other = self.coverCloseTV;
                value = self.arc.getMax();
            } else {
                other = self.coverOpenTV;
                value = 0;
            }

            try {
                stop = (Boolean) view.getTag(R.id.tag__cover__stop);
            } catch (NullPointerException exception) {
                stop = false;
            }

            try {
                handle = stop || !(Boolean) other.getTag(R.id.tag__cover__stop);
            } catch (NullPointerException exception) {
                handle = true;
            }

            if (handle && !stop)
                self.arc.setDelta(value);

            return handle;
        }
    }

    public static class ArcThumbCallback implements DrawableLoader.Callback {
        public SeekArc view;

        public ArcThumbCallback(SeekArc view) {
            this.view = view;
        }

        @Override
        public void onSuccess(Drawable drawable) {
            this.view.setThumbDrawable(drawable);
        }

        @Override
        public void onFailure(AvarioException exception) {
        }

        @Override
        public void onCancel() {
        }
    }
}
