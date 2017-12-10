package com.avariohome.avario.mqtt;


import android.content.Context;
import android.os.Handler;

import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;


/**
 * Connection object class for MQTT connections and related data. Contains callbacks so that the
 * app can be notified what is happening with the connection
 * <p>
 * Guidelines:
 * - class does not infinitely do any connection attempts and will ALWAYS stop when the maximum
 * retry amount is achieved
 * <p>
 * Created by aeroheart-c6 on 23/05/2017.
 */
public class MqttConnection implements MqttCallbackExtended, IMqttActionListener {
    public enum Status {
        CONNECTED,
        DISCONNECTED,
        ERROR,
        NONE
    }

    public enum Action {
        CONNECT,
        DISCONNECT,
        SUBSCRIBE
    }

    public static final String TAG = "Avario/MqttConnection";

    private Context context;
    private Handler mainHandler;
    private Handler workHandler;

    private Action action; // will be null when it's not currently trying to do anything
    private Status status;
    private Listener listener;
    private int retriesMax;
    private int retries;

    private String clientId;
    private String host;
    private String port;
    private boolean ssl;
    private String topic; // publish topic
    private MqttSubscription subscription;

    private MqttAndroidClient client;
    private MqttConnectOptions options;

    public MqttConnection(Context context, String clientId, String host, String port, boolean ssl) {
        this.mainHandler = Application.mainHandler;
        this.workHandler = Application.workHandler;

        this.action = null;
        this.status = Status.NONE;
        this.retriesMax = 5;
        this.retries = 1;

        this.context = context;
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.ssl = ssl;

        this.options = new MqttConnectOptions();
        this.options.setConnectionTimeout(10);
        this.options.setCleanSession(true);
        this.options.setAutomaticReconnect(false);

        this.reset();
    }

    // region utilities & property manipulation
    MqttConnection setHost(String host) {
        this.host = host;
        return this;
    }

    MqttConnection setPort(String port) {
        this.port = port;
        return this;
    }

    MqttConnection setSSL(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    MqttConnection setSubscribeTopic(String topic) {
        if (topic == null || this.subscription != null && this.subscription.topic.equals(topic))
            return this;

        this.subscription = new MqttSubscription(topic);
        return this;
    }

    MqttConnection setPublishTopic(String topic) {
        this.topic = topic;
        return this;
    }

    MqttConnection setAuthentication(String username, String password) {
        this.options.setUserName(username);
        this.options.setPassword(password.toCharArray());
        return this;
    }

    public String getHost() {
        return host;
    }

    MqttConnection setKeepAlive(int seconds) {
        this.options.setKeepAliveInterval(seconds);
        return this;
    }

    MqttConnection setRetryCount(int retry) {
        this.retries = retry;
        return this;
    }

    public Status getStatus() {
        return this.status;
    }

    public int getRetryCount() {
        return this.retries;
    }

    public int getRetryMax() {
        return this.retriesMax;
    }

    /**
     * Resets the connection. Closes an existing client before creating a new one.
     *
     * @return the {@link MqttConnection} instance
     */
    public MqttConnection reset() {
        try {
            this.client.setCallback(null);
            this.client.close();
        } catch (NullPointerException exception) {
        }

        this.client = new MqttAndroidClient(context, this.buildUrl(), this.clientId);
        this.client.setCallback(this);

        return this;
    }

    private boolean hasPendingAction() {
        if (this.action != null)
            Log.d(TAG, "Pending Action: " + this.action.name());

        return this.action != null;
    }

    private void updateStatus(Status status) {
        Status previous = this.status;

        if (previous == status && status != Status.ERROR)
            return;

        if (status == Status.DISCONNECTED)
            this.subscription.subscribed = false;

        this.status = status;
        this.dispatchOnStatusChanged(previous, status);
    }

    private String buildUrl() {
        return new StringBuilder()
                .append(this.ssl ? "ssl://" : "tcp://")
                .append(this.host)
                .append(":")
                .append(this.port)
                .toString();
    }

    // endregion

    // region event dispatch

    /**
     * registers a Listener for this connection to listen for interesting events. To unset a
     * listener for this object, pass `null` as the argument
     *
     * @param listener
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void dispatchOnConnection(final boolean reconnection) {
        if (this.listener == null)
            return;

        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;
                self.listener.onConnection(self, reconnection);
            }
        });
    }

    private void dispatchOnConnectionFailed(final AvarioException exception) {
        if (this.listener == null)
            return;

        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;
                self.listener.onConnectionFailed(self, exception);
            }
        });
    }

    private void dispatchOnDisconnection(final AvarioException exception) {
        if (this.listener == null)
            return;

        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;
                self.listener.onDisconnection(self, exception);
            }
        });
    }

    private void dispatchOnSubscription() {
        if (this.listener == null)
            return;

        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;
                self.listener.onSubscription(self);
            }
        });
    }

    private void dispatchOnSubscriptionError(final AvarioException exception) {
        if (this.listener == null)
            return;

        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;
                self.listener.onSubscriptionError(self, exception);
            }
        });
    }

    private void dispatchOnStatusChanged(final Status previous, final Status current) {
        if (this.listener == null)
            return;

        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;
                self.listener.onStatusChanged(self, previous, current);
            }
        });
    }
    // endregion

    // region main operations
    public void connect() throws MqttException {
        this.connect(false);
    }

    private void connect(boolean retry) throws MqttException {
        if (!retry && this.hasPendingAction())
            return;

        this.setRetryCount(retry ? this.getRetryCount() + 1 : 0);

        this.action = Action.CONNECT;
        this.client.connect(this.options, this.action, this);

        Log.d(TAG, "Setting action to: " + this.action.name());
    }

    public void disconnect() throws MqttException {
        if (this.hasPendingAction())
            return;

        this.action = Action.DISCONNECT;
        this.client.disconnect(this.action, this);

        Log.d(TAG, "Setting action to: " + this.action.name());
    }

    public void subscribe() throws AvarioException {
        if (this.hasPendingAction() || this.subscription == null || this.subscription.subscribed)
            return;

        this.action = Action.SUBSCRIBE;
        Log.d(TAG, "Setting action to: " + this.action.name());

        try {
            this.client.subscribe(
                    this.subscription.topic,
                    Constants.MQTT_QOS,
                    this.action,
                    this
            );
        } catch (MqttException exception) {
            throw new AvarioException(
                    Constants.ERROR_MQTT_SUBSCRIPTION,
                    exception,
                    new Object[]{this.subscription.topic}
            );
        }
    }

    // endregion

    // region MqttCallbackExtended
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.d(TAG, "connectComplete()");
        this.updateStatus(Status.CONNECTED);
        this.dispatchOnConnection(reconnect);

        try {
            this.subscribe();
        } catch (AvarioException exception) {
            this.dispatchOnSubscriptionError(exception);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "disconnected");
        this.updateStatus(Status.DISCONNECTED);

        // when disconnected as planned, `cause` is null
        this.dispatchOnDisconnection(cause == null ? null : new AvarioException(
                Constants.ERROR_MQTT_DISCONNECTED,
                cause
        ));
    }

    /**
     * Handles the message from MQTT. Note that the method runs on the main UI thread (hence the need
     * for running it in the background worker thread)
     *
     * @param topic
     * @param message
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Application.workHandler.post(new MqttMessageParser(message));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    // endregion

    // region IMqttActionListener
    @Override
    public void onSuccess(IMqttToken token) {
        if (this.action == null)
            return;

        Log.i(TAG, String.format("Action: %s Succeeded", action.name()));

        boolean unsetAction = true;

        switch (this.action) {
            case SUBSCRIBE:
                unsetAction = this.onActionSubscribe();
                break;
        }

        if (unsetAction)
            this.action = null;
    }

    @Override
    public void onFailure(IMqttToken token, Throwable throwable) {
        if (this.action == null)
            return;

        Log.i(TAG, String.format("Action: %s Failed", action.name()), throwable);

        boolean unsetAction = true;

        switch (this.action) {
            case CONNECT:
                unsetAction = this.onActionConnect((MqttException) throwable);
                break;

            case DISCONNECT:
                unsetAction = this.onActionDisconnect((MqttException) throwable);
                break;

            case SUBSCRIBE:
                unsetAction = this.onActionSubscribe(throwable);
                break;
        }

        if (unsetAction)
            this.action = null;
    }

    private boolean onActionSubscribe() {
        this.subscription.subscribed = true;
        this.dispatchOnSubscription();
        return true;
    }

    private boolean onActionSubscribe(Throwable throwable) {
        this.subscription.subscribed = false;
        this.dispatchOnSubscriptionError(new AvarioException(
                Constants.ERROR_MQTT_SUBSCRIPTION,
                throwable,
                new Object[]{this.subscription.topic}
        ));

        return true;
    }

    private boolean onActionConnect(MqttException mqttException) {
        int reasonCode = mqttException.getReasonCode();

        if (reasonCode == MqttException.REASON_CODE_CONNECT_IN_PROGRESS)
            return false;
        else if (reasonCode == MqttException.REASON_CODE_CLIENT_CONNECTED)
            return true;
        else if (reasonCode == MqttException.REASON_CODE_CLIENT_CLOSED) {
            this.reset();
            return true;
        }

        AvarioException exception = new AvarioException(
                mqttException instanceof MqttSecurityException
                        ? Constants.ERROR_MQTT_AUTHENTICATION
                        : Constants.ERROR_MQTT_CONNECTION,
                mqttException
        );

        if (this.getRetryCount() >= this.getRetryMax()) {
            this.updateStatus(Status.ERROR);
            this.dispatchOnConnectionFailed(exception);
            return true;
        }

        this.dispatchOnConnectionFailed(exception);

        this.workHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MqttConnection self = MqttConnection.this;

                try {
                    self.connect(true);
                } catch (MqttException ignored) {
                }
            }
        }, 1000);

        return false;
    }

    private boolean onActionDisconnect(MqttException exception) {
        this.updateStatus(Status.ERROR);
        return true;
    }
    // endregion

    // region Listener for users
    public interface Listener {
        /**
         * Notifies listeners that the MqttConnection object has successfully made connection
         *
         * @param connection   the connection object
         * @param reconnection whether or not this was a reconnection
         */
        void onConnection(MqttConnection connection, boolean reconnection);

        /**
         * Notifies listeners that the MqttConnection object has encountered errors when making
         * connections. This event will reemit for each failure of connection within the alloted
         * number of retries.
         *
         * @param connection the connection object
         */
        void onConnectionFailed(MqttConnection connection, AvarioException exception);

        /**
         * Notifies listeners that the MqttConnection object has been disconnected.
         *
         * @param connection the connection object
         * @param exception  whatever error occured when client was disconnected. This will be null
         *                   for clean disconnections.
         */
        void onDisconnection(MqttConnection connection, AvarioException exception);

        /**
         * Notifies listeners that the MqttConnection object has subscribed to the topic
         *
         * @param connection the connection object
         */
        void onSubscription(MqttConnection connection);

        /**
         * Notifies listeners that the MqttConnection object has been unable to subscribe.
         *
         * @param connection the connection object
         * @param exception  the error information from the subscription
         */
        void onSubscriptionError(MqttConnection connection, AvarioException exception);

        /**
         * Notifies listeners that the MqttConnection object status has been changed.
         *
         * @param connection the connection object
         * @param previous   the previous {@link Status} value
         * @param current    the current {@link Status} value
         */
        void onStatusChanged(
                MqttConnection connection,
                MqttConnection.Status previous,
                MqttConnection.Status current
        );
    }
    // endregion
}
