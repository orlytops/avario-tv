package com.avariohome.avario.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.core.APITimers;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.widget.adapter.Entity;
import com.avariohome.avario.widget.adapter.MediaAdapter;
import com.avariohome.avario.widget.adapter.RoomEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by aeroheart-c6 on 12/04/2017.
 */
public class MediaList extends RecyclerView {
    private static final String TAG = "Avario/MediaList";

    private MediaAdapter adapter;
    private Listener listener;

    public MediaList(Context context) {
        this(context, null, 0);
    }

    public MediaList(Context context, AttributeSet attributes) {
        this(context, attributes, 0);
    }

    public MediaList(Context context, AttributeSet attributes, int defaultStyleAttr) {
        super(context, attributes, defaultStyleAttr);

        if (attributes != null) {
            TypedArray array = this.getContext().obtainStyledAttributes(
                    attributes,
                    R.styleable.MediaList,
                    defaultStyleAttr,
                    0
            );

            array.recycle();
        }

        this.adapter = new MediaAdapter();
        this.listener = null;

        this.getItemAnimator().setChangeDuration(0);
        this.setHasFixedSize(true);
        this.setAdapter(this.adapter);
        this.setLayoutManager(new LinearLayoutManager(
                this.getContext(),
                LinearLayoutManager.VERTICAL,
                false
        ));
        this.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(
                    Rect rect,
                    View view,
                    RecyclerView parent,
                    RecyclerView.State state
            ) {
                float density = parent.getResources().getDisplayMetrics().density;
                int position = parent.getChildAdapterPosition(view);

                if (position < state.getItemCount() - 1)
                    rect.bottom = (int) (10.0 * density);
            }
        });
        this.addOnItemTouchListener(new ItemListener());

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());

        manager.registerReceiver(new StateReceiver(), new IntentFilter(Constants.BROADCAST_STATE_CHANGED));
        manager.registerReceiver(new TickerReceiver(), new IntentFilter(Constants.BROADCAST_MEDIA));
    }

    public void setup(List<RoomEntity> rooms) {
        StateArray states = StateArray.getInstance();
        List<String> entityIds = new ArrayList<>();
        List<Entity> entities = new ArrayList<>();

        for (RoomEntity room : rooms) {
            JSONArray mediaJSON = room.data.optJSONArray("media");

            for (int index = 0, length = mediaJSON.length(); index < length; index++) {
                String entityId = mediaJSON.optString(index);

                if (entityIds.contains(entityId))
                    continue;

                entityIds.add(entityId);
            }
        }

        // retrieve the entities from the StateArray
        for (String entityId : entityIds)
            try {
                Entity media = this.adapter.get(entityId);

                if (media == null)
                    media = new Entity();

                media.selected = false;
                media.data = states.getMediaEntity(entityId);
                media.id = media.data.optString("entity_id");

                entities.add(media);
            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(this.getContext(), exception)
                        .show();
            }

        // sort them entities
        Collections.sort(entities, new MediaPlayerComparator());

        if (entities.size() > 0) {
            Entity media;

            media = entities.get(0);
            media.selected = true;

            this.adapter.setSelected(media);
        }

        this.adapter.clear();
        this.adapter.addAll(entities);
        Log.d("Entity media size", entities.size() + "");
        this.adapter.notifyDataSetChanged();

        this.fireMediaListUpdated();

    }

    @Override
    public MediaAdapter getAdapter() {
        return this.adapter;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void fireMediaListUpdated() {
        if (this.listener != null)
            this.listener.onMediaListUpdated();
    }

    private void fireMediaSelected(Entity entity) {
        if (this.listener != null)
            this.listener.onMediaSelected(entity);
    }

    /*
     ***********************************************************************************************
     * Inner Classes
     ***********************************************************************************************
     */
    private class ItemListener extends GestureDetector.SimpleOnGestureListener
            implements RecyclerView.OnItemTouchListener {
        private GestureDetector detector;

        private ItemListener() {
            this.detector = new GestureDetector(MediaList.this.getContext(), this);
        }

        // RecyclerView.OnItemTouchListener
        @Override
        public boolean onInterceptTouchEvent(RecyclerView source, MotionEvent event) {
            this.detector.onTouchEvent(event);
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }

        // GestureDetector.SimpleOnGestureListener
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            this.handleGesture(event);
            return true;
        }


//        @Override
//        public boolean onSingleTapConfirmed(MotionEvent event) {
//            this.handleGesture(event);
//            return true;
//        }
//
//        @Override
//        public void onLongPress(MotionEvent event) {
//            this.handleGesture(event);
//        }
//
//        @Override
//        public boolean onDoubleTap(MotionEvent event) {
//            this.handleGesture(event);
//            return true;
//        }

        private void handleGesture(MotionEvent event) {
            MediaList self = MediaList.this;
            Entity media;
            int position;

            position = self.getChildAdapterPosition(self.findChildViewUnder(
                    event.getX(),
                    event.getY()
            ));

            media = self.adapter.getSelected();

            if (media != null)
                media.selected = false;

            try {
                media = self.adapter.get(position);
                media.selected = true;
            } catch (NullPointerException exception) {
                return;
            }

            self.adapter.setSelected(media);
            self.adapter.notifyItemRangeChanged(0, self.adapter.getItemCount(), MediaAdapter.BIND_SELECTION);
            self.fireMediaSelected(media);
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MediaAdapter adapter = MediaList.this.getAdapter();
            Entity selected = adapter.getSelected();
            String entityId = intent.getStringExtra("entity_id");
            StateArray states = StateArray.getInstance();

            int length = adapter.getItemCount();

            if (entityId == null) {
                for (int index = 0; index < length; index++)
                    try {
                        Entity media;

                        media = adapter.get(index);
                        media.data = states.getMediaEntity(media.id);
                        APITimers.invalidate(media.id);
                    } catch (AvarioException ignored) {
                    }

                adapter.notifyItemRangeChanged(0, length);
            } else
                for (int index = 0; index < length; index++) {
                    Entity media = adapter.get(index);

                    if (!media.id.equals(entityId))
                        continue;

                    try {
                        selected.data = states.getMediaEntity(selected.id);
                        adapter.notifyItemChanged(index);

                        try {
                            String state = adapter.getSelected().data
                                    .getJSONObject("new_state")
                                    .getString("state");
                            if (listener != null) {
                                listener.onMediaState(state);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        APITimers.invalidate(media.id);
                    } catch (AvarioException ignored) {
                    }
                }
        }
    }

    private class TickerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MediaAdapter adapter = MediaList.this.getAdapter();

            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), MediaAdapter.BIND_SEEK);
        }
    }

    private class MediaPlayerComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity objectA, Entity objectB) {
            JSONObject jsonA = objectA.data,
                    jsonB = objectB.data;
            int priorityA = jsonA.optInt("priority", -1),
                    priorityB = jsonB.optInt("priority", -1);

            if (priorityA > priorityB)
                return 1;
            else if (priorityA < priorityB)
                return -1;
            else
                return jsonA.optString("name").compareTo(jsonB.optString("name"));
        }
    }

    public interface Listener {
        /**
         * called after setting up the media list widget. I currently do not recall how this
         * callback plays a role in anything -- but there is!
         */
        void onMediaListUpdated();

        /**
         * called after selecting a media player in the list. Multi-selections are NOT supported.
         *
         * @param entity
         */
        void onMediaSelected(Entity entity);

        void onMediaState(String state);
    }
}
