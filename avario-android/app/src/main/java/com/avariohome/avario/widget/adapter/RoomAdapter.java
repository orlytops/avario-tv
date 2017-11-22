package com.avariohome.avario.widget.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.avariohome.avario.R;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.util.AssetUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aeroheart-c6 on 12/12/2016.
 */
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {
    public static final String TAG = "Avario/RoomAdapter";
    public static final String BIND_SELECTION_ROOM = "selection__room";
    public static final String BIND_SELECTION_MEDIA = "selection__media";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROOM = 1;

    private ItemClickListener itemListener;

    private List<RoomEntity> rooms;

    private int headerLayoutId;
    private int itemLayoutId;

    public RoomAdapter() {
        super();

        this.rooms = new ArrayList<>();
        this.headerLayoutId = R.layout.roomselector__item__header;
        this.itemLayoutId = R.layout.roomselector__item;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ROOM;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        if (type == TYPE_HEADER) {
            view = inflater.inflate(this.headerLayoutId, parent, false);

            AssetUtil.loadImage(
                    parent.getContext(),
                    R.array.ic__roomselect__device,
                    new AssetUtil.ImageViewCallback((ImageView) view.findViewById(R.id.device)),
                    (ImageView) view.findViewById(R.id.device)
            );

            AssetUtil.toDrawable(
                    parent.getContext(),
                    R.array.ic__roomselect__media,
                    new AssetUtil.ImageViewCallback((ImageView) view.findViewById(R.id.media))
            );
        } else {
            view = inflater.inflate(this.itemLayoutId, parent, false);

            AssetUtil.toDrawable(
                    parent.getContext(),
                    R.array.ic__roomselect__indicator,
                    new AssetUtil.ImageViewCallback((ImageView) view.findViewById(R.id.media))
            );
        }

        return new ViewHolder(view, this.itemListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        } else if (position == 0)
            return;

        for (Object payload : payloads)
            if (payload instanceof String && payload.equals(RoomAdapter.BIND_SELECTION_ROOM))
                this.bindSelection(holder, this.get(position - 1));
            else if (payload instanceof String && payload.equals(RoomAdapter.BIND_SELECTION_MEDIA))
                this.bindSelectionMedia(holder, this.get(position - 1));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position == 0)
            return;

        RoomEntity room = this.rooms.get(position - 1);

        holder.roomTV.setText(room.name);
        this.bindSelection(holder, room);
        this.bindSelectionMedia(holder, room);
    }

    private void bindSelection(ViewHolder holder, RoomEntity room) {
        holder.roomTV.setSelected(false);
    }

    private void bindSelectionMedia(ViewHolder holder, RoomEntity room) {
        Config config = Config.getInstance();
        String name = config.getRoomSelected();

        if (name == null) {
            holder.mediaIV.setSelected(room.selectedMedia);
        } else if (name.equals(room.name)) {
            holder.mediaIV.setSelected(true);
            room.selectedMedia = true;
        } else {
            holder.mediaIV.setSelected(false);
            room.selectedMedia = false;
        }
    }

    public List<RoomEntity> getMediaSelections() {
        List<RoomEntity> rooms = new ArrayList<>();
        for (RoomEntity room : this.rooms)
            if (room.selectedMedia)
                rooms.add(room);

        return rooms;
    }

    public RoomEntity getRoomSelection() {
        for (RoomEntity room : this.rooms)
            if (room.selected)
                return room;

        return null;
    }

    public void clearRoomSelection() {
        for (RoomEntity room : this.rooms)
            room.selected = false;
    }

    @Override
    public int getItemCount() {
        return this.rooms.size() + 1;
    }

    public int size() {
        return this.rooms.size();
    }

    public void clear() {
        this.rooms.clear();
    }

    public RoomEntity get(int index) {
        try {
            return this.rooms.get(index);
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }
    }

    public void add(RoomEntity room) {
        this.rooms.add(room);
    }

    public void setItemClickListener(ItemClickListener itemListener) {
        this.itemListener = itemListener;
    }

    /*
     ***********************************************************************************************
     * Inner Classes - View Holders
     ***********************************************************************************************
     */
    static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private ItemClickListener listener;
        private TextView roomTV;
        private ImageView mediaIV;

        private ViewHolder(View view, ItemClickListener listener) {
            super(view);

            this.listener = listener;

            this.roomTV = (TextView) view.findViewById(R.id.title);
            this.mediaIV = (ImageView) view.findViewById(R.id.media);

            this.itemView.setOnClickListener(this);
            this.mediaIV.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (this.getItemViewType() == TYPE_HEADER || this.listener == null)
                return;

            this.listener.onItemClick(this.itemView, view == this.mediaIV, getAdapterPosition());
        }
    }

    /*
     ***********************************************************************************************
     * Interfaces
     ***********************************************************************************************
     */
    public interface ItemClickListener {
        void onItemClick(View item, boolean media, int position);
    }
}
