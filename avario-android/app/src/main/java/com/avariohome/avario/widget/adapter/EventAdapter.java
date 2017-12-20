package com.avariohome.avario.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.avariohome.avario.R;
import com.avariohome.avario.util.AssetUtil;

import java.util.List;

/**
 * Created by orly on 11/1/17.
 */

public class EventAdapter extends BaseAdapter {

    private Context context;
    private List<Integer> listImageUrl;

    public EventAdapter(Context context, List<Integer> listImageUrl) {
        this.listImageUrl = listImageUrl;
        this.context = context;
    }

    @Override
    public int getCount() {
        return listImageUrl.size();
    }

    @Override
    public Object getItem(int position) {
        return listImageUrl.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.item_events, parent, false);
        }

        ImageView eventImage = (ImageView)
                convertView.findViewById(R.id.image_event);

        int image = listImageUrl.get(position);

        AssetUtil.toDrawable(convertView.getContext(), image, null);

        return convertView;
    }
}
