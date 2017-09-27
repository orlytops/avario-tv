package com.avariohome.avario.widget.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.AssetLoaderTask;
import com.avariohome.avario.util.EntityUtil;
import com.avariohome.avario.util.PlatformUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aeroheart-c6 on 16/12/2016.
 */
public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
    public static final String BIND_SEEK = "seek";
    public static final String BIND_SELECTION = "selection";

    private StateArray states;
    private List<Entity> mediaEntities;
    private Entity selectedEntity;

    public MediaAdapter() {
        super();
        this.mediaEntities = new ArrayList<>();
        this.states = StateArray.getInstance();
    }

    @Override
    public int getItemCount() {
        return this.mediaEntities.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.nowplaying__item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }

        for (Object payload : payloads)
            if (payload instanceof String && payload.equals(MediaAdapter.BIND_SEEK))
                this.bindSeek(holder, this.mediaEntities.get(position));
            else if (payload instanceof String && payload.equals(MediaAdapter.BIND_SELECTION))
                this.bindSelection(holder, this.mediaEntities.get(position));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Entity entity = this.mediaEntities.get(position);
        JSONObject entityJSON = entity.data;

        this.bindSelection(holder, entity);

        try {
            String state = entity.data
                .getJSONObject("new_state")
                .getString("state");

            holder.rootLayout.setVisibility(
                !state.equals(Constants.ENTITY_MEDIA_STATE_PLAYING) && !state.equals(Constants.ENTITY_MEDIA_STATE_PAUSED)
                    ? View.GONE
                    : View.VISIBLE

            );
        }
        catch (JSONException exception) {
            holder.itemView.setVisibility(View.VISIBLE);
        }

        // Thumbnail
        try {
            String url = this.states.getHTTPHost("ip1") + entityJSON
                .getJSONObject("new_state")
                .getJSONObject("attributes")
                .getString("entity_picture");

            AssetLoaderTask.picasso(holder.itemView.getContext())
                .load(url)
                .fit()
                .centerCrop()
                .into(holder.thumbnailIV);
        }
        catch (AvarioException | JSONException exception) {
            holder.thumbnailIV.setImageDrawable(null);
        }

        // Title
        try {
            holder.titleTV.setText(
                entityJSON
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getString("media_title")
            );
        }
        catch (JSONException exception) {
            holder.titleTV.setText("");

            /*PlatformUtil.logError(new AvarioException(
                Constants.ERROR_STATE_MISSINGKEY,
                exception,
                new Object[] {
                    String.format("%s.new_state.attributes.media_title", entityJSON.optString("entity_id"))
                })
            );*/
        }

        // Artist
        try {
            holder.artistTV.setText(
                entityJSON
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getString("media_artist")
            );
        }
        catch (JSONException exception) {
            holder.artistTV.setText("");

            /*PlatformUtil.logError(new AvarioException(
                Constants.ERROR_STATE_MISSINGKEY,
                exception,
                new Object[] {
                    String.format("%s.new_state.attributes.media_artist", entityJSON.optString("entity_id"))
                })
            );*/
        }

        this.bindSeek(holder, entity);
    }

    private void bindSelection(ViewHolder holder, Entity entity) {
        holder.itemView.setSelected(entity.selected);
    }

    private void bindSeek(ViewHolder holder, Entity entity) {
        JSONObject entityJSON = entity.data;

        // Media Position
        try {
            holder.seekTV.setText(PlatformUtil.toTimeString(EntityUtil.getSeekPosition(entityJSON)));
        }
        catch (AvarioException exception) {
            holder.seekTV.setText(R.string.medialist__unknown__timer);
        }

        // Media Duration
        try {
            holder.durationTV.setText(PlatformUtil.toTimeString(
                entityJSON
                    .getJSONObject("new_state")
                    .getJSONObject("attributes")
                    .getInt("media_duration")
            ));
        }
        catch (JSONException exception) {
            holder.durationTV.setText(R.string.medialist__unknown__timer);

            /*PlatformUtil.logError(new AvarioException(
                Constants.ERROR_STATE_MISSINGKEY,
                exception,
                new Object[] {
                    String.format("%s.new_state.attributes.media_duration", entityJSON.optString("entity_id"))
                })
            );*/
        }
    }

    public void clear() {
        this.mediaEntities.clear();
    }

    public void add(Entity mediaEntity) {
        this.mediaEntities.add(mediaEntity);
    }

    public void addAll(List<Entity> mediaEntities) {
        this.mediaEntities.addAll(mediaEntities);
    }

    public Entity get(int index) {
        try {
            return this.mediaEntities.get(index);
        }
        catch (ArrayIndexOutOfBoundsException exception) {
            return null;
        }
    }

    public Entity get(String entityId) {
        for (Entity media : this.mediaEntities)
            if (media.id.equals(entityId))
                return media;

        return null;
    }

    public List<Entity> getAll() {
        return this.mediaEntities;
    }

    public void setSelected(Entity entity) {
        this.selectedEntity = entity;
    }

    public Entity getSelected() {
        return this.selectedEntity;
    }

    /*
     ***********************************************************************************************
     * Inner Classes - SubTypes
     ***********************************************************************************************
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rootLayout;
        private ImageView thumbnailIV;
        private TextView titleTV;
        private TextView artistTV;
        private TextView seekTV;
        private TextView durationTV;

        private ViewHolder(View view) {
            super(view);

            this.rootLayout = (RelativeLayout) view.findViewById(R.id.root);
            this.thumbnailIV = (ImageView)view.findViewById(R.id.thumbnail);
            this.titleTV = (TextView)view.findViewById(R.id.title);
            this.artistTV = (TextView)view.findViewById(R.id.artist);
            this.seekTV = (TextView)view.findViewById(R.id.seek);
            this.durationTV = (TextView)view.findViewById(R.id.duration);
        }
    }
}
