package com.example;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttClientService implements MqttCallback {
    static final Logger logger = LoggerFactory.getLogger(MqttClientService.class);

    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "data/#";

    private MqttClient client;

    public MqttClientService() throws MqttException {
        this.client = new MqttClient(BROKER, CLIENT_ID);
        client.setCallback(this);
    }

    public void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        client.connect(options);
        client.subscribe(TOPIC);
    }

    public void disconnect() throws MqttException {
        client.disconnect();
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.info("Connection lost: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        logger.info("Received message: {}", payload);
        // 추가 처리 로직...
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.info("Message delivery complete: {}", token.getMessageId());
    }
}
