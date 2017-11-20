package com.avario.home.widget.adapter;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avario.home.R;
import com.avario.home.core.Notification;
import com.avario.home.fragment.NotifListDialogFragment;
import com.avario.home.util.PlatformUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by memengski on 7/11/17.
 */

public class NotifListAdapter extends RecyclerView.Adapter<NotifListAdapter.ViewHolder> {

  private List<Notification> notifications;
  private NotifListDialogFragment.Listener listener;

  public NotifListAdapter(List<Notification> notifications,
      NotifListDialogFragment.Listener listener)  {
    super();
    this.notifications = notifications;
    this.listener = listener;
  }

  @Override
  public int getItemCount() {
    return this.notifications.size();
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.notif__list__item, parent, false);

    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, final int position) {
    JSONObject jsonObject = notifications.get(position).data;

    boolean isRead;
    try {
      isRead = jsonObject.getBoolean("is_read");
    } catch (JSONException exception) {
      isRead = false;
    }

    try {
      holder.titleTV.setText(jsonObject.getString("title"));
      setReadUI(holder.titleTV, isRead);
    } catch (JSONException exception) {}

    try {
      holder.messageTV.setText(jsonObject.getString("body"));
      setReadUI(holder.messageTV, isRead);
    } catch (JSONException exception) {}

    try {
      holder.collapseKeyTV.setText(jsonObject.getString("merge_id"));
      setReadUI(holder.collapseKeyTV, isRead);
    } catch (JSONException exception) {
      holder.collapseKeyTV.setVisibility(View.GONE);
    }
    try {
      holder.dateTV.setText(PlatformUtil.toDateTimeString(jsonObject.getLong("date_sent")));
      setReadUI(holder.dateTV, isRead);
    } catch (JSONException ignored) {}

    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        listener.onSelectedItem(notifications.get(position));
      }
    });
  }

  private void setReadUI(TextView textView, boolean isRead) {
    textView.setTypeface(null, isRead ? Typeface.NORMAL : Typeface.BOLD);
  }

  /*
    ***********************************************************************************************
    * Inner Classes - SubTypes
    ***********************************************************************************************
    */
  static class ViewHolder extends RecyclerView.ViewHolder {
    private TextView       titleTV;
    private TextView       messageTV;
    private TextView       collapseKeyTV;
    private TextView       dateTV;

    private ViewHolder(View view) {
      super(view);

      this.titleTV = (TextView)view.findViewById(R.id.title);
      this.messageTV = (TextView)view.findViewById(R.id.message);
      this.collapseKeyTV = (TextView)view.findViewById(R.id.collapse__key);
      this.dateTV = (TextView)view.findViewById(R.id.date);
    }
  }
}
