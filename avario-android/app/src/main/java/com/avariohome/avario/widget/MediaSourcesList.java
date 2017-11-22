package com.avariohome.avario.widget;


import android.content.Context;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.widget.adapter.Entity;
import com.avariohome.avario.widget.adapter.MediaSourceAdapter;
import com.avariohome.avario.widget.adapter.RoomEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by aeroheart-c6 on 19/04/2017.
 */
public class MediaSourcesList extends RecyclerView {
    private MediaSourceAdapter adapter;
    private Listener listener;

    public MediaSourcesList(Context context) {
        this(context, null, 0);
    }

    public MediaSourcesList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaSourcesList(Context context, AttributeSet attrs, int defaultStyleAttr) {
        super(context, attrs, defaultStyleAttr);

        this.listener = null;

        this.setAdapter(new MediaSourceAdapter());
        this.setHasFixedSize(true);
        this.setLayoutManager(new LinearLayoutManager(
            this.getContext(),
            LinearLayoutManager.VERTICAL,
            false
        ));
        this.addItemDecoration(new DividerItemDecoration(
            this.getContext(),
            LinearLayoutManager.VERTICAL
        ));
        this.addOnItemTouchListener(new ItemListener());
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
                Entity media;

                media = new Entity();
                media.selected = false;
                media.data = states.getMediaEntity(entityId);
                media.id = media.data.optString("entity_id");

                entities.add(media);
            }
            catch (AvarioException exception) {
                PlatformUtil
                    .getErrorToast(this.getContext(), exception)
                    .show();
            }

        // sort them entities
        Collections.sort(entities, new MediaPlayerComparator());

        // add appropriate items into the adapter
        // entityIds no lnoger needed, will be refurbished
        entityIds = new ArrayList<>();

        for (Entity media : entities) {
            try {
                JSONArray sourcesJSON = media.data.getJSONArray("media_sources");

                for (int index = 0, length = sourcesJSON.length(); index < length; index++) {
                    if (entityIds.contains(sourcesJSON.get(index)))
                        continue;

                    entityIds.add(sourcesJSON.getString(index));
                }
            }
            catch (JSONException exception) {}
        }

        this.adapter.clear();
        this.adapter.addAll(entityIds);
        this.adapter.notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setAdapter(MediaSourceAdapter adapter) {
        super.setAdapter(adapter);
        this.adapter = adapter;
    }

    @Override
    public MediaSourceAdapter getAdapter() {
        return this.adapter;
    }

    private void fireMediaSourceSelected(String name) {
        if (this.listener == null)
            return;

        try {
            String appId = StateArray
                .getInstance()
                .getMediaSourceAppId(name);

            this.listener.onMediaSourceSelected(name, appId);
        }
        catch (AvarioException exception) {
            PlatformUtil
                .getErrorToast(this.getContext(), exception)
                .show();
        }


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
            this.detector = new GestureDetector(MediaSourcesList.this.getContext(), this);
        }

        // GestureDetector.SimpleOnGestureListener
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            MediaSourcesList self = MediaSourcesList.this;

            int position = self.getChildAdapterPosition(self.findChildViewUnder(
                event.getX(),
                event.getY()
            ));

            self.fireMediaSourceSelected(self.adapter.get(position));

            return true;
        }

        // RecyclerView.OnItemTouchListener
        @Override
        public boolean onInterceptTouchEvent(RecyclerView source, MotionEvent event) {
            return source.findChildViewUnder(event.getX(), event.getY()) != null
                && this.detector.onTouchEvent(event);
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {}

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
    }

    private class MediaPlayerComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity objectA, Entity objectB) {
            JSONObject jsonA = objectA.data,
                jsonB = objectB.data;
            int priorityA = jsonA.optInt("priority", -1),
                priorityB = jsonB.optInt("priority", -1);

            if (priorityA > priorityB)
                return -1;
            else if (priorityA < priorityB)
                return 1;
            else
                return jsonA.optString("name").compareTo(jsonB.optString("name"));
        }
    }

    public interface Listener {
        void onMediaSourceSelected(String name, String appId);
    }
}
