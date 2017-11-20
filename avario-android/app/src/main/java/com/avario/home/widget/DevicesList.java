package com.avario.home.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avario.home.Constants;
import com.avario.home.R;
import com.avario.home.core.APITimers;
import com.avario.home.core.StateArray;
import com.avario.home.exception.AvarioException;
import com.avario.home.util.AssetUtil;
import com.avario.home.util.EntityUtil;
import com.avario.home.widget.adapter.DeviceAdapter;
import com.avario.home.widget.adapter.Entity;
import com.avario.home.widget.event.TapDetector;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


/**
 * Displays each item that will be affected by the changes in the dial. Supports multi-selection.
 * <p>
 * Created by aeroheart-c6 on 15/12/2016.
 */
public class DevicesList extends RelativeLayout {
    private RelativeLayout headerRL;
    private RelativeLayout cancelRL;
    private RecyclerView devicesRV;
    private TextView cancelTV;


    private DeviceAdapter adapter;
    private Listener listener;
    private int headerHeight;
    private int headerId;
    private int contentMargin;
    private int selectCount;
    private boolean multiselectMode;

    public DevicesList(Context context) {
        this(context, null, 0);
    }

    public DevicesList(Context context, AttributeSet attributes) {
        this(context, attributes, 0);
    }

    public DevicesList(Context context, AttributeSet attributes, int defaultStyleAttr) {
        super(context, attributes, defaultStyleAttr);

        float density = this.getResources().getDisplayMetrics().density;

        int headerHeight = (int) (48.00 * density),
                contentMargin = (int) (5 * density);

        if (attributes != null) {
            TypedArray array = this.getContext().obtainStyledAttributes(
                    attributes,
                    R.styleable.DevicesList,
                    defaultStyleAttr,
                    0
            );

            headerHeight = array.getDimensionPixelSize(
                    R.styleable.DevicesList_headerHeight,
                    headerHeight
            );
            contentMargin = array.getDimensionPixelSize(
                    R.styleable.DevicesList_contentMargin,
                    contentMargin
            );

            array.recycle();
        }

        this.headerId = View.generateViewId();
        this.headerHeight = headerHeight;
        this.contentMargin = contentMargin;
        this.multiselectMode = false;
        this.selectCount = 0;

        // this.adapter = new DeviceAdapter(new ItemListener());
        this.adapter = new DeviceAdapter();

        this.initializeHead();
        this.initializeBody();

        LocalBroadcastManager
                .getInstance(this.getContext())
                .registerReceiver(new StateReceiver(), new IntentFilter(Constants.BROADCAST_STATE_CHANGED));
    }

    private void initializeHead() {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        LayoutParams params;

        this.headerRL = (RelativeLayout) inflater.inflate(R.layout.deviceslist__header, this, false);

        params = (RelativeLayout.LayoutParams) this.headerRL.getLayoutParams();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = this.headerHeight;

        this.headerRL.setId(this.headerId);
        this.headerRL.setVisibility(View.INVISIBLE);
        this.headerRL.setLayoutParams(params);
        this.cancelRL = (RelativeLayout) this.headerRL.findViewById(R.id.cancel);
        this.cancelRL.setOnClickListener(new ClickListener());

        this.cancelTV = (TextView) this.cancelRL.findViewById(R.id.text);


        AssetUtil.loadImage(
                this.getContext(),
                R.array.ic__deviceslist__back,
                new AssetUtil.ImageViewCallback((ImageView) this.headerRL.findViewById(R.id.icon)),
                this.headerRL.findViewById(R.id.icon)
        );
        this.addView(this.headerRL);
    }

    private void initializeBody() {
        Context context = this.getContext();
        LayoutParams params;
        RecyclerView.ItemAnimator animator;

        params = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        params.addRule(ALIGN_PARENT_BOTTOM, TRUE);
        params.addRule(BELOW, this.headerId);

        this.devicesRV = new RecyclerView(context);
        this.devicesRV.addOnItemTouchListener(new ItemListener(this.getContext()));
        this.devicesRV.addItemDecoration(new ItemDecoration());
        this.devicesRV.setLayoutParams(params);
        this.devicesRV.setHasFixedSize(true);
        this.devicesRV.setLayoutManager(new LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
        ));
        this.devicesRV.setAdapter(this.adapter);

        animator = this.devicesRV.getItemAnimator();
        animator.setChangeDuration(0);
        animator.setAddDuration(0);
        animator.setRemoveDuration(0);

        this.addView(this.devicesRV);
    }

    private void updateSelections() {
        this.selectCount = this.adapter.getSelected().size();
    }

    private void updateMode() {
        if (this.selectCount <= 0)
            this.toggleMultiselectMode(false);

        if (this.multiselectMode)
            this.updateSelectionHeader();
    }

    public void updateSelectionHeader() {
        this.cancelTV.setText(this.getResources().getString(
                R.string.deviceslist__selected,
                this.selectCount
        ));
    }

    public void toggleMultiselectMode(boolean enabled) {
        this.headerRL.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        this.multiselectMode = enabled;
    }

    public void clearSelections() {
        this.selectCount = 0;
        this.adapter.clearSelected();
    }

    public void clearSelections(Entity device) {
        this.selectCount = 0;
        this.adapter.clearSelected(device);
    }

    private boolean isMultiselectable() {
        return this.getAdapter().size() > 1;
    }

    public boolean isSelectionAllowed(Entity device, String type) {
        JSONArray commandsJSON = EntityUtil.getGUICommands(device.data, type);

        if (commandsJSON == null)
            return false;

        for (int index = 0, length = commandsJSON.length(); index < length; index++)
            if (commandsJSON.optString(index).equals("A"))
                return true;

        return false;
    }

    public DeviceAdapter getAdapter() {
        return this.adapter;
    }

    /**
     * Highlights the entities specified in the JSONArray. ignores JSONExceptions it comes across
     * silently
     *
     * @param entityIds a JSONArray of strings containing entity Ids
     * @return a List of Strings that been selected based on checks done by this object
     * @todo create a counterpart that accepts a List<String> parameter
     */
    public List<String> setSelections(JSONArray entityIds) {
        DeviceAdapter adapter = this.getAdapter();
        List<Entity> entities = adapter.getAll();
        List<String> passers = new ArrayList<>();
        int length = entityIds.length();

        for (int index = 0; index < length; index++) {
            String entityId = entityIds.optString(index, null);

            if (entityId == null)
                continue;

            for (Entity entity : entities)
                if (entity.id.equals(entityId) && this.isSelectionAllowed(entity, "clk")) {
                    entity.selected = true;
                    passers.add(entityId);
                    break;
                }
        }

        this.toggleMultiselectMode(passers.size() > 1);
        this.updateSelections();
        this.updateSelectionHeader();

        adapter.notifyItemRangeChanged(0, entities.size());

        return passers;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void fireSelectionCleared() {
        if (this.listener == null)
            return;

        this.listener.onSelectionsCleared(this);
    }

    private void fireSelectionChanged() {
        if (this.listener == null)
            return;

        this.listener.onSelectionsChanged(this);
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Events
     ***********************************************************************************************
     */
    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            DevicesList self = DevicesList.this;

            if (view.getId() == R.id.cancel) {
                self.toggleMultiselectMode(false);
                self.clearSelections();
                self.updateSelectionHeader();
                self.adapter.notifyItemRangeChanged(0, self.adapter.getItemCount());

                self.fireSelectionCleared();
            }
        }
    }

    private class ItemListener extends GestureDetector.SimpleOnGestureListener
            implements RecyclerView.OnItemTouchListener {

        TapDetector detector;

        ItemListener(Context context) {
            this.detector = new TapDetector(context, this);
        }

        // region RecyclerView.OnItemTouchListener
        @Override
        public boolean onInterceptTouchEvent(RecyclerView source, MotionEvent event) {
            return this.detector.onTouchEvent(event);
        }

        @Override
        public void onTouchEvent(RecyclerView source, MotionEvent event) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
        // endregion

        // region GestureDetector.SimpleOnGestureListener
        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            DevicesList self = DevicesList.this;
            View view = self.devicesRV.findChildViewUnder(event.getX(), event.getY());

            if (view == null)
                return super.onSingleTapConfirmed(event);

            SelectionData data = this.getSelectionData(view);
            boolean multiselect = self.multiselectMode;

            if (self.isSelectionAllowed(data.entity, "clk")) {
                if (!multiselect) {
                    self.clearSelections(data.entity);
                    data.entity.selected = true;
                } else
                    data.entity.selected = !data.entity.selected;

                self.adapter.notifyItemRangeChanged(
                        0,
                        self.adapter.getItemCount(),
                        DeviceAdapter.BIND_SELECTION
                );

                self.updateSelections();
            }

            self.updateMode();

            if (self.selectCount <= 0)
                self.fireSelectionCleared();
            else
                self.fireSelectionChanged();

            // run the entity's API command
            if (!multiselect)
                EntityUtil.runAPICommands(data.entity.data, "clk", self.getContext());

            return super.onSingleTapConfirmed(event);
        }


        @Override
        public boolean onDoubleTap(MotionEvent event) {
            DevicesList self = DevicesList.this;
            View view = self.devicesRV.findChildViewUnder(event.getX(), event.getY());

            if (view == null)
                return super.onDoubleTap(event);

            SelectionData data = this.getSelectionData(view);
            boolean multiselect = self.multiselectMode;

            if (self.isSelectionAllowed(data.entity, "dbl")) {
                if (!multiselect) {
                    self.clearSelections(data.entity);
                    data.entity.selected = true;
                } else
                    data.entity.selected = !data.entity.selected;

                self.adapter.notifyItemRangeChanged(
                        0,
                        self.adapter.getItemCount(),
                        DeviceAdapter.BIND_SELECTION
                );

                self.updateSelections();
            }

            self.updateMode();

            // run the entity's API commands
            if (!multiselect)
                EntityUtil.runAPICommands(data.entity.data, "dbl", self.getContext());

            return super.onDoubleTap(event);
        }

        @Override
        public void onLongPress(MotionEvent event) {
            DevicesList self = DevicesList.this;
            View view = self.devicesRV.findChildViewUnder(event.getX(), event.getY());

            if (view == null) {
                super.onLongPress(event);
                return;
            }

            SelectionData data = this.getSelectionData(view);

            if (self.isSelectionAllowed(data.entity, "lng")) {
                if (!self.multiselectMode && self.isMultiselectable()) {
                    self.clearSelections();
                    self.toggleMultiselectMode(true);
                }

                data.entity.selected = !self.multiselectMode || !data.entity.selected;
                self.adapter.notifyItemRangeChanged(
                        0,
                        self.adapter.getItemCount(),
                        DeviceAdapter.BIND_SELECTION
                );

                self.updateSelections();
            }

            self.updateMode();

            if (self.selectCount <= 0)
                self.fireSelectionCleared();
            else
                self.fireSelectionChanged();

            super.onLongPress(event);
        }
        // endregion

        private SelectionData getSelectionData(View view) {
            RecyclerView devicesRV = DevicesList.this.devicesRV;
            DeviceAdapter adapter = DevicesList.this.adapter;
            SelectionData data;

            data = new SelectionData();
            data.position = devicesRV.getChildAdapterPosition(view);
            data.entity = adapter.get(data.position);

            return data;
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            StateArray states = StateArray.getInstance();
            DeviceAdapter adapter = DevicesList.this.getAdapter();

            String entityId = intent.getStringExtra("entity_id");
            int length = adapter.size(),
                    index;

            if (entityId == null) {
                for (index = 0; index < length; index++) {
                    try {
                        Entity device;

                        device = adapter.get(index);
                        device.data = states.getEntity(device.id);
                        APITimers.invalidate(device.id);
                    } catch (AvarioException ignored) {
                    }
                }

                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            } else {
                for (index = 0; index < length; index++) {
                    Entity device = adapter.get(index);

                    if (!device.id.equals(entityId))
                        continue;

                    try {
                        String oldState,
                                newState;

                        device.data = states.getEntity(device.id);

                        try {
                            oldState = device.data
                                    .getJSONObject("old_state")
                                    .getString("state");
                        } catch (JSONException exception) {
                            oldState = "";
                        }

                        try {
                            newState = device.data
                                    .getJSONObject("new_state")
                                    .getString("state");
                        } catch (JSONException exception) {
                            newState = "";
                        }

                        if (oldState.equals(newState))
                            adapter.notifyItemChanged(index, DeviceAdapter.BIND_VALUE);
                        else
                            adapter.notifyItemChanged(index);
                        APITimers.invalidate(device.id);
                    } catch (AvarioException ignored) {
                    }
                }
            }
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - SubTypes
     ***********************************************************************************************
     */
    private class SelectionData {
        int position;
        Entity entity;
    }

    private static class ItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            Drawable divider = parent.getContext()
                    .getResources()
                    .getDrawable(R.drawable.ic__divider);

            DeviceAdapter adapter = (DeviceAdapter) parent.getAdapter();

            int inset = parent
                    .getContext()
                    .getResources()
                    .getDimensionPixelSize(R.dimen.deviceslist__inset),
                    insetDivider = parent
                            .getContext()
                            .getResources()
                            .getDimensionPixelSize(R.dimen.deviceslist__inset__divider);

            int left = 0 + insetDivider,
                    right = parent.getWidth() - insetDivider - inset;

            for (int index = 0, length = parent.getChildCount() - 1; index < length; index++) {
                View child = parent.getChildAt(index);
                Entity item;

                int position = parent.getChildAdapterPosition(child);
                int top, bottom;

                if (position < 0)
                    continue;

                item = adapter.get(position);
                top = child.getBottom();
                bottom = top + divider.getIntrinsicHeight();

                if (item.selected)
                    divider.setState(new int[]{android.R.attr.state_selected});
                else
                    divider.setState(new int[]{});

                divider.setBounds(left, top, right, bottom);
                divider.draw(canvas);
            }
        }
    }

    public interface Listener {
        /**
         * Triggered when the shortcut button for deselecting all items is pressed or when all items
         * have been manually deselected
         */
        void onSelectionsCleared(DevicesList source);

        /**
         * Triggered when a selection in the list changes. Note that there could be nothing
         */
        void onSelectionsChanged(DevicesList source);
    }
}
