package com.avariohome.avario.widget.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.EntityUtil;
import com.avariohome.avario.util.RefStringUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by aeroheart-c6 on 15/12/2016.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    public static final String TAG = "Avaro/DeviceAdapter";
    public static final String BIND_SELECTION = "selection";
    public static final String BIND_VALUE = "value";

    public static final int MODE_HOME = 1;
    public static final int MODE_CLIMATE = 2;

    private List<Entity> devices;
    private int itemLayoutId;
    private int mode;

    public DeviceAdapter() {
        this.itemLayoutId = R.layout.deviceslist__item;
        this.devices = new ArrayList<>();
        this.mode = DeviceAdapter.MODE_HOME;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(this.itemLayoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }

        for (Object payload : payloads)
            if (payload instanceof String && payload.equals(DeviceAdapter.BIND_SELECTION))
                this.bindSelection(holder, this.devices.get(position));
            else if (payload instanceof String && payload.equals(DeviceAdapter.BIND_VALUE))
                this.bindValue(holder, this.devices.get(position));

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Entity entity = this.devices.get(position);
        String[] imageUrls;

        this.bindSelection(holder, entity);
        this.bindValue(holder, entity);

        if (this.mode == DeviceAdapter.MODE_HOME)
            holder.nameTV.setText(entity.data.optString("name"));
        else
            holder.nameTV.setText(entity.data.optString(
                    "name_climate",
                    entity.data.optString("name"))
            );

        try {
            imageUrls = EntityUtil.getStateIconUrl(
                    holder.itemView.getContext(),
                    entity.data
            );
        } catch (AvarioException exception) {
            imageUrls = new String[]{""};
        }

        try {
            AssetUtil.loadImage(
                    holder.itemView.getContext(),
                    imageUrls,
                    new AssetUtil.ImageViewCallback(holder.iconIV),
                    holder.iconIV
            );
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        //holder.iconIV.setImageDrawable(null);
    }

    private void bindValue(ViewHolder holder, Entity entity) {
        String value;
        String type = "";

        try {
            value = RefStringUtil.processCode(entity.data.getString("value"));
        } catch (AvarioException | JSONException exception) {
            value = "";
        }

        try {
            type = entity.data.optString("type");
            float tmp;

            tmp = Float.parseFloat(value);
            tmp = type.equals("cover")
                    ? tmp / Constants.MAX_VALUE_COVER * 100.00f
                    : tmp / Constants.MAX_VALUE_NUMBER * 100.00f;

            value = String.valueOf(Math.round(tmp));
        } catch (NumberFormatException ignored) {
        }

        //Log.d("Device value", value + " " + type);
        holder.valueTV.setText(value);
    }

    private void bindSelection(ViewHolder holder, Entity entity) {
        holder.itemView.setSelected(entity.selected);
        holder.itemView.setActivated(EntityUtil.isEntityOn(entity.data));
    }


    /**
     * Flags all selections as false. No exclusions. No nothing.
     */
    public void clearSelected() {
        this.clearSelected(null);
    }

    /**
     * Flags all selections as false, but excluding the specified DevicesList.Device
     * instance
     *
     * @param device the device to ignore when iterating through the list
     */
    public void clearSelected(Entity device) {
        for (Entity item : this.devices) {
            if (device != null && item == device)
                continue;

            item.selected = false;
        }
    }

    public List<Entity> getSelected() {
        List<Entity> selected = new ArrayList<>();

        for (Entity device : this.devices)
            if (device.selected)
                selected.add(device);

        return selected;
    }

    public void setMode(int mode) {
        if (mode != DeviceAdapter.MODE_HOME &&
                mode != DeviceAdapter.MODE_CLIMATE)
            mode = DeviceAdapter.MODE_HOME;

        this.mode = mode;
    }

    @Override
    public int getItemCount() {
        return this.devices.size();
    }

    public int size() {
        return this.devices.size();
    }

    public void clear() {
        this.devices.clear();
    }

    public void append(Entity device) {
        this.devices.add(device);
    }

    public void remove(int index) {
        this.devices.remove(index);
    }

    public Entity get(int index) {
        try {
            return this.devices.get(index);
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }
    }

    /**
     * Returns all the items represented in the adapter as an unmodifiable list
     *
     * @return all items in the adapter
     */
    public List<Entity> getAll() {
        return Collections.unmodifiableList(this.devices);
    }

    /*
     ***********************************************************************************************
     * Inner Classes
     ***********************************************************************************************
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconIV;
        private TextView nameTV;
        private TextView valueTV;

        private ViewHolder(View view) {
            super(view);

            this.nameTV = (TextView) view.findViewById(R.id.name);
            this.valueTV = (TextView) view.findViewById(R.id.value);
            this.iconIV = (ImageView) view.findViewById(R.id.icon);
        }
    }
}
