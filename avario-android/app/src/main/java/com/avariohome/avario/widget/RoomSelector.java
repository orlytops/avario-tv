package com.avariohome.avario.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.widget.adapter.RoomAdapter;
import com.avariohome.avario.widget.adapter.RoomEntity;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aeroheart-c6 on 11/12/2016.
 */
public class RoomSelector extends RelativeLayout {
    public static final String TAG = "Avario/RoomSelector";

    private RelativeLayout handleRL;
    private RecyclerView roomsRV;
    private View dummyView;

    // handle children views
    private TextView titleTV;
    private ImageView indicatorIV;

    // view properties
    private RoomAdapter adapter;
    private SelectionListener listener;
    private RoomEntity selectedRoom;

    private int widgetWidth;
    private int handleId;
    private int handleHeight;
    private int handleBG;
    private int bodyBG;
    private boolean opened;
    private boolean handleIsRes;
    private boolean bodyIsRes;

    public RoomSelector(Context context) {
        this(context, null, 0);
    }

    public RoomSelector(Context context, AttributeSet attributes) {
        this(context, attributes, 0);
    }

    public RoomSelector(Context context, AttributeSet attributes, int defaultStyleAttr) {
        super(context, attributes, defaultStyleAttr);

        TypedValue values = new TypedValue();
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();

        int handleResId = R.layout.roomselector__handle,
                width,
                height,
                handleBG,
                bodyBG;
        boolean handleIsRes,
                bodyIsRes;

        if (this.getContext().getTheme().resolveAttribute(R.attr.actionBarSize, values, true))
            height = TypedValue.complexToDimensionPixelSize(values.data, metrics);
        else
            height = (int) (48.0f * metrics.density);

        width = (int) (150.0f * metrics.density);

        handleIsRes = true;
        handleBG = -1;

        bodyIsRes = true;
        bodyBG = -1;

        if (attributes != null) {
            TypedArray array = this.getContext().obtainStyledAttributes(
                    attributes,
                    R.styleable.RoomSelector,
                    defaultStyleAttr,
                    0
            );

            width = array.getDimensionPixelSize(R.styleable.RoomSelector_widgetWidth, width);
            height = array.getDimensionPixelSize(R.styleable.RoomSelector_handleHeight, height);

            if ((handleBG = array.getResourceId(R.styleable.RoomSelector_handleBG, handleBG)) == -1) {
                handleBG = array.getColor(R.styleable.RoomSelector_handleBG, Color.TRANSPARENT);
                handleIsRes = false;
            }

            if ((bodyBG = array.getResourceId(R.styleable.RoomSelector_bodyBG, bodyBG)) == -1) {
                bodyBG = array.getColor(R.styleable.RoomSelector_bodyBG, Color.TRANSPARENT);
                bodyIsRes = false;
            }

            array.recycle();
        }

        this.handleId = View.generateViewId();
        this.handleHeight = height;
        this.handleBG = handleBG;
        this.handleIsRes = handleIsRes;
        this.bodyBG = bodyBG;
        this.bodyIsRes = bodyIsRes;
        this.widgetWidth = width;

        this.adapter = new RoomAdapter();
        this.adapter.setItemClickListener(new ClickListener());

        this.initializeHandle(handleResId, height);
        this.initializeBody();
        this.initializeAssets();

        this.bringChildToFront(this.handleRL);
        this.close();

        IntentFilter filter;

        filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_STATE_CHANGED);
        filter.addAction(Constants.BROADCAST_ROOM_CHANGED);

        LocalBroadcastManager
                .getInstance(this.getContext())
                .registerReceiver(new StateReceiver(), filter);
    }

    private void initializeHandle(int layoutResId, int height) {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        LayoutParams layout;
        ClickListener listener;

        listener = new ClickListener();

        layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        this.dummyView = new View(this.getContext());
        this.dummyView.setId(View.generateViewId());
        this.dummyView.setLayoutParams(layout);
        this.dummyView.setBackgroundColor(Color.TRANSPARENT);
        this.dummyView.setOnClickListener(listener);

        this.addView(this.dummyView);

        layout = new LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT);

        this.handleRL = (RelativeLayout) inflater.inflate(layoutResId, null);
        this.handleRL.setLayoutParams(layout);
        this.handleRL.setId(this.handleId);
        this.handleRL.setOnClickListener(listener);

        if (PlatformUtil.isLollipopOrNewer())
            this.handleRL.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        /*if (this.handleIsRes)
            this.handleRL.setBackgroundResource(this.handleBG);
        else
            this.handleRL.setBackgroundColor(this.handleBG);*/

        this.indicatorIV = (ImageView) this.handleRL.findViewById(R.id.indicator);
        this.titleTV = (TextView) this.handleRL.findViewById(R.id.title);

        this.addView(this.handleRL);
    }

    private void initializeBody() {
        LayoutParams layout;
        DividerItemDecoration decoration;

        layout = new LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layout.addRule(RelativeLayout.BELOW, this.handleId);

        decoration = new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL);
        decoration.setDrawable(
                this.getResources()
                        .getDrawable(R.drawable.ic__divider__roomselect)
        );

        this.roomsRV = new RecyclerView(this.getContext());
        this.roomsRV.getItemAnimator().setChangeDuration(0);
        this.roomsRV.setLayoutParams(layout);
        this.roomsRV.setAdapter(this.adapter);
        this.roomsRV.setHasFixedSize(true);
        this.roomsRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.roomsRV.addItemDecoration(decoration);

        if (PlatformUtil.isLollipopOrNewer())
            this.roomsRV.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        if (this.bodyIsRes)
            this.roomsRV.setBackgroundResource(this.bodyBG);
        else
            this.roomsRV.setBackgroundColor(this.bodyBG);
    }

    private void initializeAssets() {
        AssetUtil.toDrawable(
                this.getContext(),
                R.array.ic__roomselect__more,
                new AssetUtil.ImageViewCallback((ImageView) this.handleRL.findViewById(R.id.indicator))
        );

        AssetUtil.toDrawable(
                this.getContext(),
                R.array.ic__roomselect__locator,
                new AssetUtil.ImageViewCallback((ImageView) this.handleRL.findViewById(R.id.location))
        );
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (!this.opened) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(this.widgetWidth, MeasureSpec.EXACTLY);
            heightSpec = View.MeasureSpec.makeMeasureSpec(this.handleHeight, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthSpec, heightSpec);
    }

    public void toggle() {
        if (this.opened) {
            this.close();
            handleRL.setBackgroundColor(getResources().getColor(R.color.trasnparent));
        } else {
            this.open();
            handleRL.setBackgroundColor(getResources().getColor(R.color.white__90));
        }
    }

    private void open() {
        this.addView(this.roomsRV);
        this.handleRL.setActivated(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float density = this.getResources().getDisplayMetrics().density;
            float elevation = 4.0f * density;

            this.handleRL.setElevation(elevation);
            this.handleRL.setTranslationZ(elevation);
            this.roomsRV.setElevation(elevation);
            this.roomsRV.setTranslationZ(elevation);
        }

        this.opened = true;

        this.invalidate();
        this.requestLayout();
    }

    private void close() {
        this.removeView(this.roomsRV);
        this.handleRL.setActivated(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.handleRL.setElevation(0.0f);
            this.handleRL.setTranslationZ(0.0f);
            this.roomsRV.setElevation(0.0f);
            this.roomsRV.setTranslationZ(0.0f);
        }

        this.opened = false;

        this.invalidate();
        this.requestLayout();
    }

    public void setup() throws AvarioException {
        JSONArray roomsJSON = StateArray.getInstance().getRooms();
        int count = this.adapter.size();

        this.adapter.clear();
        this.adapter.notifyItemRangeRemoved(0, count);

        for (int index = 0, limit = roomsJSON.length(); index < limit; index++) {
            JSONObject roomJSON = roomsJSON.optJSONObject(index);
            RoomEntity room;

            room = new RoomEntity();
            room.data = roomJSON;
            room.id = roomJSON.optString("entity_id");
            room.name = roomJSON.optString("name");
            room.selected = this.selectedRoom != null && room.id.equals(this.selectedRoom.id);

            this.adapter.add(room);
            this.adapter.notifyItemInserted(index);
        }
    }

    public void setupSelected() {
        RoomEntity room = this.adapter.getRoomSelection();

        if (room != null && room.id.equals(this.selectedRoom.id))
            return;

        // no selected room? get first room
        if (room == null && (this.adapter.size()) > 0)
            room = this.adapter.get(0);

        this.setSelectedRoom(room);
    }

    public void dispatchRoomSelected() {
        handleRL.setBackgroundColor(getResources().getColor(R.color.trasnparent));
        if (this.listener != null)
            this.listener.onRoomSelected(this, this.selectedRoom);
    }

    public void dispatchMediaSelected() {
        if (this.listener != null)
            this.listener.onRoomMediaSelected(this);
    }

    public RoomAdapter getAdapter() {
        return this.adapter;
    }

    public void setSelectedRoom(RoomEntity room) {
        if (room == null)
            return;

        if (this.selectedRoom != null)
            try {
                this.selectedRoom
                        .data
                        .put("active", false);
                this.selectedRoom.selected = false;
            } catch (JSONException exception) {
                return;
            }

        try {
            room.data
                    .put("active", true);
        } catch (JSONException exception) {
            return;
        }

        this.selectedRoom = room;
        this.selectedRoom.selected = true;

        this.setTitle(this.selectedRoom.name);

        this.dispatchRoomSelected();
    }

    public RoomEntity getSelectedRoom() {
        return this.selectedRoom;
    }

    public void setTitle(CharSequence title) {
        this.titleTV.setText(title);
    }

    public void setTitle(int resourceId) {
        this.titleTV.setText(resourceId);
    }

    @Override
    public void setActivated(boolean activated) {
        this.indicatorIV.setActivated(activated);

        super.setActivated(activated);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.handleRL.setEnabled(enabled);
        if (enabled) {
            this.indicatorIV.setVisibility(View.VISIBLE);
        } else {
            this.indicatorIV.setVisibility(View.INVISIBLE);
            this.close();
        }

        super.setEnabled(enabled);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Events
     ***********************************************************************************************
     */
    private class ClickListener implements View.OnClickListener,
            RoomAdapter.ItemClickListener {
        @Override
        public void onClick(View view) {
            RoomSelector self = RoomSelector.this;
            if (view.getId() == self.dummyView.getId()) {
                self.close();
            } else {
                self.toggle();
            }
        }

        @Override
        public void onItemClick(View item, boolean media, int position) {
            RoomSelector self = RoomSelector.this;
            RoomEntity room = self.adapter.get(self.roomsRV.getChildAdapterPosition(item) - 1);
            String payload;

            if (media) {
                payload = RoomAdapter.BIND_SELECTION_MEDIA;
                room.selectedMedia = !room.selectedMedia;
                self.dispatchMediaSelected();
                Config config = Config.getInstance();
                Gson gson = new Gson();
                if (room.selectedMedia) {
                    List<String> roomsSelected = new ArrayList<>();
                    if (config.getRoomSelected() != null) {
                        roomsSelected.addAll(config.getRoomSelected());
                    }
                    roomsSelected.add(room.name);
                    String selectedRoomJson = gson.toJson(roomsSelected);
                    config.setRoomSelected(selectedRoomJson);
                } else {
                    List<String> roomsSelected = config.getRoomSelected();
                    roomsSelected.remove(room.name);
                    String selectedRoomJson = gson.toJson(roomsSelected);
                    config.setRoomSelected(selectedRoomJson);
                }
            } else {
                payload = RoomAdapter.BIND_SELECTION_ROOM;

                self.close();
                self.setSelectedRoom(room);
            }

            self.adapter.notifyItemRangeChanged(0, self.adapter.getItemCount(), payload);
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.BROADCAST_ROOM_CHANGED))
                this.handleRoomChange(intent);
            else if (action.equals(Constants.BROADCAST_STATE_CHANGED))
                this.handleStateChange(intent);
        }

        private void handleRoomChange(Intent intent) {
            StateArray states = StateArray.getInstance();
            String roomId = intent.getStringExtra("entity_id");
            RoomEntity room;

            if (roomId == null)
                return;

            try {
                room = new RoomEntity();
                room.data = states.getRoom(roomId);
                room.id = roomId;
                room.name = room.data.optString("name");
                room.selected = false;
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(RoomSelector.this.getContext(), exception)
                        .show();

                return;
            }

            RoomSelector.this.setSelectedRoom(room);
        }

        private void handleStateChange(Intent intent) {
            if (intent.getStringExtra("entity_id") != null)
                return;

            RoomSelector self = RoomSelector.this;
            RoomAdapter adapter = self.getAdapter();

            for (int index = 0, limit = adapter.size(); index < limit; index++) {
                RoomEntity room = adapter.get(index);
                JSONObject roomJSON;

                try {
                    roomJSON = StateArray.getInstance().getRoom(room.id);
                } catch (AvarioException exception) {
                    continue;
                }

                room.data = roomJSON;
                room.name = roomJSON.optString("name");
            }

            adapter.notifyItemRangeChanged(0, adapter.getItemCount());

            if (!self.isEnabled())
                return;

            self.setupSelected();

            try {
                self.setTitle(self.getSelectedRoom().name);
            } catch (NullPointerException ignored) {
            }
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - SubTypes
     ***********************************************************************************************
     */
    public interface SelectionListener {
        void onRoomSelected(RoomSelector selector, RoomEntity room);

        /**
         * On media selections, it is encouraged instead to get the selected media rooms through
         * the adapter (@see RoomAdapter.getMediaSelections()) when listening to this event because
         * the widget does not keep a list of rooms with selected media
         *
         * @param selector
         */
        void onRoomMediaSelected(RoomSelector selector);
    }
}
