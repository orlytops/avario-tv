package com.avariohome.avario.fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.avariohome.avario.R;
import com.avariohome.avario.core.Notification;
import com.avariohome.avario.core.NotificationArray;
import com.avariohome.avario.widget.adapter.NotificationListAdapter;

/**
 * Created by memengski on 7/11/17.
 */

public class NotificationListDialogFragment extends DialogFragment {
    private static final String TAG = "Avario/NotifListDialog";

    private Listener listener;

    private NotificationListAdapter adapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = this.getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(this.setupViews(LayoutInflater.from(activity)));

        AlertDialog dialog = builder.create();

        this.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialogTheme);
        this.renderMessage(dialog);

        return dialog;
    }

    @Override
    public void onDetach() {
        if (this.listener != null)
            this.listener.onDialogDetached();

        super.onDetach();
    }

    private View setupViews(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.fragment__notif__list__dialog, null, false);

        Button removeAllButton = (Button) view.findViewById(R.id.button__remove);
        removeAllButton.setOnClickListener(new ClickListener());

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerView.getContext(),
                layoutManager.getOrientation()
        );

        recyclerView.addItemDecoration(dividerItemDecoration);

        this.adapter = new NotificationListAdapter(
                NotificationArray.getInstance().getNotifications(),
                listener);
        recyclerView.setAdapter(this.adapter);
        return view;
    }

    private void renderMessage(Dialog dialog) {
        dialog.setTitle("Notifications");
    }

    public void refresh() {
        this.adapter.notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private class ClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            NotificationListDialogFragment self = NotificationListDialogFragment.this;

            NotificationArray.getInstance().getNotifications().clear();
            self.dismiss();
        }
    }

    public interface Listener {
        void onDialogDetached();

        void onSelectedItem(Notification notification);
    }
}
