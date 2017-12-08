package com.avariohome.avario.fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIRequestListener;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.Notification;
import com.avariohome.avario.core.NotificationArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.RefStringUtil;

import org.eclipse.paho.client.mqttv3.internal.websocket.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Dialog to be shown when receiving the push notification from Firebase.
 * <p>
 * Created by aeroheart-c6 on 07/07/2017.
 */
public class NotificationDialogFragment extends DialogFragment {
    private static final String TAG = "Avario/NotifDialog";
    private static final String TIMER_ID = "notifdialog";

    /*
    To: null
    From: 359276913279
    Type: null
    Message Id: 0:1499410989044167%39a5f54239a5f542
    Collapse Key: com.avariohome.avario
    Notification:
        Title: Message Title
        Body: Message Text
    Payload:
    {buttons="{\"ha!\":\"{\\\"hello\\\":\\\"world\\\"}\"}"}
     */

    private LinearLayout buttonsLL;
    private ScrollView messageHolderSV;
    private TextView messageTV;
    private WebView webview;
    private Button closeB;

    private ClickListener clickListener;
    private Listener listener;
    private Notification notification;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.clickListener = new ClickListener();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = this.getArguments();
        Notification notification = arguments.getParcelable("notification");

        Activity activity = this.getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(this.setupViews(LayoutInflater.from(activity)));

        AlertDialog dialog = builder.create();

        this.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialogTheme);
        this.renderMessage(dialog, notification);

        return dialog;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        if (this.listener != null)
            this.listener.onDialogDetached();

        super.onDetach();
    }

    public void resetArguments(Bundle arguments) {
        Notification notification = arguments.getParcelable("notification");

        this.renderMessage(
                this.getDialog(),
                notification
        );
    }

    private void renderMessage(Dialog dialog, Notification notification) {
        if (notification == null)
            return;

        this.notification = notification;

        try {
            dialog.setTitle(notification.data.getString("title"));
            this.messageTV.setText(notification.data.getString("body"));

            JSONArray buttonsJSON = notification.data.getJSONArray("buttons");
            this.setupButtons(buttonsJSON);
            this.messageTV.setOnClickListener(buttonsJSON.length() == 0 ? this.clickListener : null);
        } catch (JSONException ignored) {
        }

        this.buttonsLL.setVisibility(View.VISIBLE);
        this.messageHolderSV.setVisibility(View.VISIBLE);

        this.closeB.setVisibility(View.GONE);
        this.webview.setVisibility(View.GONE);
    }

    private View setupViews(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.fragment__notifdialog, null, false);

        this.messageHolderSV = (ScrollView) view.findViewById(R.id.message__holder);
        this.messageTV = (TextView) view.findViewById(R.id.message);
        this.buttonsLL = (LinearLayout) view.findViewById(R.id.buttons__holder);
        this.webview = (WebView) view.findViewById(R.id.webview);
        this.closeB = (Button) view.findViewById(R.id.close);

        this.closeB.setOnClickListener(this.clickListener);

        return view;
    }

    /**
     * Creates and adds the buttons according to the "buttons" directive received from the
     * push notification
     *
     * @param buttonsJSON
     */
    private void setupButtons(JSONArray buttonsJSON) {
        this.buttonsLL.removeAllViews();
        this.buttonsLL.setWeightSum(buttonsJSON.length() > 0 ? buttonsJSON.length() : 3);

        for (int index = 0; index < buttonsJSON.length(); index++) {
            JSONObject buttonJSON;

            try {
                buttonJSON = buttonsJSON.getJSONObject(index);
            } catch (JSONException exception) {
                continue;
            }

            this.buttonsLL.addView(this.createButton(buttonJSON));
        }
    }

    /**
     * Creates the individual buttons
     */
    private Button createButton(JSONObject buttonJSON) {
        LinearLayout.LayoutParams params;
        Button button;

        params = new LinearLayout.LayoutParams(
                0,
                this.getResources().getDimensionPixelSize(R.dimen.dialog__notifbutton__height)
        );
        params.weight = 1;

        button = new Button(this.getActivity());
        button.setLayoutParams(params);
        button.setText(buttonJSON.optString("txt"));
        button.setTag(R.id.tag__notifbtn__data, buttonJSON);
        button.setOnClickListener(this.clickListener);

        return button;
    }

    private void sendAPI(JSONObject buttonJSON) {
        JSONObject requestJSON;

        try {
            requestJSON = new JSONObject(
                    buttonJSON
                            .getJSONObject("api")
                            .toString()
            );
        } catch (JSONException exception) {
            return;
        }

        try {
            APIClient.getInstance().executeRequest(
                    requestJSON,
                    NotificationDialogFragment.TIMER_ID,
                    NotificationDialogFragment.TIMER_ID,
                    new APIListener()
            );
        } catch (AvarioException ignored) {
        }
    }

    /**
     * Tries to delete the notification linked to this button when it is true. Does nothing otherwise
     *
     * @param buttonJSON
     */
    private void deleteOnClick(JSONObject buttonJSON) {
        Notification notification = this.getArguments().getParcelable("notification");

        if (!buttonJSON.optBoolean("delete_on_click", false) || notification == null)
            return;

        NotificationArray
                .getInstance()
                .deleteNotification(notification);
    }

    /**
     * Tries to open the webview when the payload permits it. If, in anyway, throws an exception,
     * then assume that the webview will not be opened and it will be closed.
     *
     * @param buttonJSON
     * @throws Exception
     */
    private void openWebview(JSONObject buttonJSON) throws Exception {
        JSONObject webviewJSON;
        final Map<String, String> urlConf;

        webviewJSON = new JSONObject(
                buttonJSON
                        .getJSONObject("webview")
                        .toString()
        );

        urlConf = RefStringUtil.processUrl(webviewJSON.getString("url"));

        this.buttonsLL.setVisibility(View.GONE);
        this.messageHolderSV.setVisibility(View.GONE);

        this.closeB.setVisibility(View.VISIBLE);
        this.webview.setVisibility(View.VISIBLE);
        Log.d("URL Notification", urlConf.get("url"));
        final Config config = Config.getInstance();

        final Map<String, String> headers = new HashMap<>();

        headers.put("Authorization", String.format("Basic %s", Base64.encode(String.format(
                "%s:%s",
                config.getUsername(),
                config.getPassword()
        ))));

        Log.d("Authorization", String.format("Basic %s", Base64.encode(String.format(
                "%s:%s",
                config.getUsername(),
                config.getPassword()
        ))));
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                android.util.Log.d(TAG, "onReceivedHttpAuthRequest: ");
                handler.proceed("avario", "avario");
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        this.webview.loadUrl(urlConf.get("url"));
    }

    public String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            if (extension.equals("js")) {
                return "text/javascript";
            } else if (extension.equals("woff")) {
                return "application/font-woff";
            } else if (extension.equals("woff2")) {
                return "application/font-woff2";
            } else if (extension.equals("ttf")) {
                return "application/x-font-ttf";
            } else if (extension.equals("eot")) {
                return "application/vnd.ms-fontobject";
            } else if (extension.equals("svg")) {
                return "image/svg+xml";
            }
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /*
     ***********************************************************************************************
     * Listeners
     ***********************************************************************************************
     */
    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            NotificationDialogFragment self = NotificationDialogFragment.this;
            JSONObject buttonJSON;

            if (view.getId() == R.id.close || view.getId() == R.id.message) {
                self.dismiss();
                return;
            }

            buttonJSON = (JSONObject) view.getTag(R.id.tag__notifbtn__data);

            self.sendAPI(buttonJSON);
            self.deleteOnClick(buttonJSON);

            try {
                self.openWebview(buttonJSON);
            } catch (Exception exception) {
                self.dismiss();
            }
        }
    }

    private class APIListener extends APIRequestListener<String> {
        public APIListener() {
            super(
                    NotificationDialogFragment.TIMER_ID,
                    new String[]{NotificationDialogFragment.TIMER_ID}
            );
        }

        protected void forceTimerExpire() {
        }

        protected void startTimer() {
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Interfaces
     ***********************************************************************************************
     */
    public interface Listener {
        void onDialogDetached();
    }
}
