package com.avario.home.widget.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avario.home.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aeroheart-c6 on 15/12/2016.
 */
public class MediaSourceAdapter extends RecyclerView.Adapter<MediaSourceAdapter.ViewHolder> {
    protected static final String TAG = "Avario/Nagivation";

    private ArrayList<String> items;

    public MediaSourceAdapter() {
        super();
        this.items = new ArrayList<>();
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.navigation__item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.nameTV.setText(this.items.get(position));
    }

    public void clear() {
        this.items.clear();
    }

    public void addAll(List<String> items) {
        this.items.addAll(items);
    }

    public String get(int index) {
        return this.items.get(index);
    }


    /*
     ***********************************************************************************************
     * Inner Classes - View Holders
     ***********************************************************************************************
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTV;

        private ViewHolder(View view) {
            super(view);
            this.nameTV = (TextView)view.findViewById(R.id.title);
        }
    }
}
