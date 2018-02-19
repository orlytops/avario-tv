package com.tv.avario.mqtt;


/**
 * Created by aeroheart-c6 on 13/02/2017.
 */
public class MqttSubscription {
    public String topic;
    public boolean subscribed;

    public MqttSubscription(String topic) {
        super();

        this.topic = topic;
        this.subscribed = false;
    }
}
