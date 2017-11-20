package com.avario.home.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.avario.home.Constants;
import com.avario.home.R;
import com.avario.home.core.APITimers;
import com.avario.home.core.StateArray;
import com.avario.home.exception.AvarioException;
import com.avario.home.util.EntityUtil;
import com.avario.home.widget.adapter.ElementAdapter;
import com.avario.home.widget.adapter.Entity;
import com.avario.home.widget.event.TapDetector;

import org.json.JSONArray;


/**
 * Created by aeroheart-c6 on 08/12/2016.
 */
public class ElementsBar extends RecyclerView {
    public static final String TAG = "Avario/PresetsBar";

    private ElementAdapter adapter;
    private Listener listener;
    private int pageSize;

    public ElementsBar(Context context) {
        this(context, null, 0);
    }

    public ElementsBar(Context context, AttributeSet attributes) {
        this(context, attributes, 0);
    }

    public ElementsBar(Context context, AttributeSet attributes, int defaultStyleAttr) {
        super(context, attributes, defaultStyleAttr);

        ItemAnimator animator;
        int pageSize = 10;

        if (attributes != null) {
            TypedArray array = this.getContext().obtainStyledAttributes(
                attributes,
                R.styleable.ElementsBar,
                defaultStyleAttr,
                0
            );

            pageSize = array.getInteger(R.styleable.ElementsBar_pageSize, this.pageSize);

            array.recycle();
        }

        this.pageSize = pageSize;
        this.adapter = new ElementAdapter(this.getContext(), this.pageSize);

        this.addOnItemTouchListener(new ItemListener(this.getContext()));
        this.setHasFixedSize(true);
        this.setLayoutManager(new LinearLayoutManager(
            this.getContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        ));
        this.setAdapter(this.adapter);

        animator = this.getItemAnimator();
        animator.setChangeDuration(0);
        animator.setAddDuration(0);
        animator.setRemoveDuration(0);

        LocalBroadcastManager
            .getInstance(this.getContext())
            .registerReceiver(new StateReceiver(), new IntentFilter(Constants.BROADCAST_STATE_CHANGED));
    }

    /*
    Don't delete until I can figure out a much.much. cleaner way to do item width adjustment.
    Something that is more closely linked to the containing RecyclerView rather than basing on
    the entire screen

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int parentWidth = View.getDefaultSize(this.getSuggestedMinimumWidth(), widthSpec);
        int padding, width;

        width = parentWidth / this.pageSize;
        padding = width / 2 / (this.pageSize - 1);

        width = width + padding;

        super.onMeasure(widthSpec, heightSpec);

        this.adapter.setItemWidth(width);
        this.adapter.notifyDataSetChanged();
    }
    */

    @Override
    public ElementAdapter getAdapter() {
        return this.adapter;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void fireDialCommand(Entity entity) {
        if (this.listener == null)
            return;

        this.listener.onDialCommand(this, entity);
    }

    private void fireListCommand(Entity entity) {
        if (this.listener == null)
            return;

        this.listener.onListCommand(this, entity);
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Listeners
     ***********************************************************************************************
     */
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
        public void onTouchEvent(RecyclerView source, MotionEvent event) {}

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}

        // endregion

        // region GestureDetector.SimpleOnGestureListener
        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            ElementsBar self = ElementsBar.this;
            ElementAdapter adapter = self.getAdapter();
            View view = self.findChildViewUnder(event.getX(), event.getY());

            if (view != null) {
                Entity entity = adapter.get(self.getChildAdapterPosition(view));

                this.runGUICommands(entity, view, "clk");
                EntityUtil.runAPICommands(entity.data, "clk", self.getContext());
            }

            return super.onSingleTapConfirmed(event);
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            ElementsBar self = ElementsBar.this;
            ElementAdapter adapter = self.getAdapter();
            View view = self.findChildViewUnder(event.getX(), event.getY());

            if (view != null) {
                Entity entity = adapter.get(self.getChildAdapterPosition(view));

                this.runGUICommands(entity, view, "dbl");
                EntityUtil.runAPICommands(entity.data, "dbl", self.getContext());
            }

            return super.onDoubleTap(event);
        }

        @Override
        public void onLongPress(MotionEvent event) {
            ElementsBar self = ElementsBar.this;
            ElementAdapter adapter = self.getAdapter();
            View view = self.findChildViewUnder(event.getX(), event.getY());

            if (view != null) {
                Entity entity = adapter.get(self.getChildAdapterPosition(view));

                this.runGUICommands(entity, view, "lng");
                EntityUtil.runAPICommands(entity.data, "lng", self.getContext());
            }

            super.onLongPress(event);
        }
        // endregion

        /**
         * Runs the GUI commands of the involved entity
         *
         * @param entity the Entity in the adapter
         * @param view the item view in the RecyclerView
         * @param type the kind of interaction being done
         */
        private void runGUICommands(Entity entity, View view, String type) {
            ElementsBar self = ElementsBar.this;
            JSONArray commandsJSON = EntityUtil.getGUICommands(entity.data, type);

            if (commandsJSON == null)
                return;

            for (int index = 0, limit = commandsJSON.length(); index < limit; index++) {
                String item = commandsJSON.optString(index);

                switch (item) {
                    case "H":
                        this.selectItem(view);
                        break;

                    case "D":
                        self.fireDialCommand(entity);
                        break;

                    case "L":
                        self.fireListCommand(entity);
                        break;
                }
            }
        }

        private void selectItem(View view) {
            ElementsBar self = ElementsBar.this;
            ElementAdapter adapter = self.getAdapter();
            Entity entity = adapter.get(self.getChildAdapterPosition(view));

            if (entity == null)
                return; // nothing to highlight

            entity.selected = !entity.selected;
            adapter.clearSelected(entity);
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), ElementAdapter.BIND_SELECTION);
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            StateArray states = StateArray.getInstance();
            ElementAdapter adapter = ElementsBar.this.getAdapter();

            String entityId = intent.getStringExtra("entity_id");
            int length = adapter.getItemCount(),
                index;

            if (entityId == null) {
                for (index = 0; index < length; index++) {
                    try {
                        Entity element;

                        element = adapter.get(index);
                        element.data = states.getEntity(element.id);
                        APITimers.invalidate(element.id);
                    }
                    catch (AvarioException ignored) {
                    }
                }

                adapter.notifyItemRangeChanged(0, length);
            }
            else {
                for (index = 0; index < length; index++) {
                    Entity element = adapter.get(index);

                    if (!element.id.equals(entityId))
                        continue;

                    try {
                        element.data = states.getEntity(element.id);
                        adapter.notifyItemChanged(index);
                        APITimers.invalidate(element.id);
                    }
                    catch (AvarioException ignored) {}
                }
            }
        }
    }

    public interface Listener {
        /**
         * Triggered when the selected entity has the "D" GUI command
         */
        void onDialCommand(ElementsBar view, Entity entity);

        /**
         * Triggered when the selected entity has the "L" GUI command
         */
        void onListCommand(ElementsBar view, Entity entity);
    }
}
