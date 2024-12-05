package com.example;

import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "data/#";
    private static MqttClient client;
    private static MqttConnectOptions options;
    private static MessageData messageData;
    private static InfluxDBService influxDBService;

    public static void main(String[] args) throws InterruptedException {
        try {
            // MQTT 클라이언트 초기화
            client = new MqttClient(BROKER, CLIENT_ID);
            
            options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.info("Connection lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    messageData.handleMessageArrived(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 메시지 전달 완료 처리
                    logger.info("Message delivered to topic: {}", token.getTopics()[0]);
                }
            });

            // InfluxDBService 초기화
            influxDBService = new InfluxDBService();

            // MQTT 서버에 연결
            connectToBroker();

            // 연결 후, 토픽 구독
            logger.info("Subscribing to topic: {}", TOPIC);
            client.subscribe(TOPIC);

            // 사용자 입력을 받아 종료하도록 처리
            Scanner scanner = new Scanner(System.in);
            String input = "";
            while (!input.equalsIgnoreCase("exit")) {
                logger.info("Type 'exit' to disconnect...");
                input = scanner.nextLine(); // 사용자가 "exit" 입력할때 까지 대기
            }

            //연결종료 처리
            logger.info("Exiting and disconnecting...");
            client.disconnect();

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // MQTT 브로커에 연결하는 메소드
    private static void connectToBroker() {
        try {
            if (!client.isConnected()) {
                logger.info("Connecting to broker...");
                client.connect(options);
                logger.info("Connected to broker!");
            }
        } catch (MqttException e) {
            logger.info("Error connecting to broker: {}", e.getMessage());
            e.printStackTrace();
            try {
                // 연결 실패 시 재시도
                Thread.sleep(5000);  // 5초 후 재시도
                connectToBroker();   // 재귀적으로 재시도
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
