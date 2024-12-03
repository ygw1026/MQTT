package com.example;

import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    private static final String BROKER = "tcp://192.168.70.203:1883";
    private static final String CLIENT_ID = "JavaClientExample";
    private static final String TOPIC = "data/#";
    private static MqttClient client;
    private static MqttConnectOptions options;
    private static LogService logService = new LogService();
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
                    System.out.println("Connection lost: " + cause.getMessage());
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    handleMessageArrived(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 메시지 전달 완료 처리
                    System.out.println("Message delivered to topic: " + token.getTopics()[0]);
                }
            });

            // InfluxDBService 초기화
            influxDBService = new InfluxDBService();

            // MQTT 서버에 연결
            connectToBroker();

            // 연결 후, 토픽 구독
            System.out.println("Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC);

            // 사용자 입력을 받아 종료하도록 처리
            Scanner scanner = new Scanner(System.in);
            String input = "";
            while (!input.equalsIgnoreCase("exit")) {
                System.out.println("Type 'exit' to disconnect...");
                input = scanner.nextLine(); // 사용자가 "exit" 입력할때 까지 대기
            }

            //연결종료 처리
            System.out.println("Exiting and disconnecting...");
            client.disconnect();

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 메시지 수신 처리 메서드
    private static void handleMessageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());

            // "lora"로 끝나는 토픽은 저장하지 않도록 처리
            if (topic.endsWith("lora")) {
                System.out.println("Ignoring message from topic '" + topic + "' because it ends with 'lora'");
                return;
            }

            // 토픽을 '/'로 분리하여 새로운 형식의 토픽으로 정제
            String[] topicParts = topic.split("/");
            String newTopic = null;

            if (topicParts.length > 12) {
                if (topicParts.length > 14) {
                    newTopic = topicParts[6] + "/" + topicParts[12] + "/" + topicParts[14];
                } else {
                    newTopic = topicParts[6] + "/" + topicParts[10] + "/" + topicParts[12];
                }
            }

            // 메시지에서 'value' 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);

            double value = 0.0;
            if (jsonNode != null && jsonNode.has("value")) {
                value = jsonNode.get("value").asDouble();
            }

            // InfluxDB에 데이터 기록
            if (newTopic != null) {
                String[] tagName = newTopic.split("/");
                influxDBService.writeToInfluxDB(tagName, value);
            }

            // 로그 파일에 메시지 기록
            logService.writeLogToFile(topic);
            logService.writeLogToFile(payload);

        } catch (Exception e) {
            System.out.println("Error processing message from topic '" + topic + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // MQTT 브로커에 연결하는 메소드
    private static void connectToBroker() {
        try {
            if (!client.isConnected()) {
                System.out.println("Connecting to broker...");
                client.connect(options);
                System.out.println("Connected to broker!");
            }
        } catch (MqttException e) {
            System.out.println("Error connecting to broker: " + e.getMessage());
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

    // 연결이 끊어졌을 때 자동으로 재연결 시도하는 메소드
    private static void reconnect() {
        int retryCount = 0;
        int maxRetries = 5;  // 최대 재시도 횟수
        while (!client.isConnected() && retryCount < maxRetries) {
            try {
                retryCount++;
                System.out.println("Attempting to reconnect... (Attempt " + retryCount + " of " + maxRetries + ")");
                Thread.sleep(5000);  // 5초 후 재연결 시도
                connectToBroker();   // 연결 시도
            } catch (InterruptedException | MqttException e) {
                System.out.println("Reconnect failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        if (retryCount == maxRetries) {
            System.out.println("Failed to reconnect after " + maxRetries + " attempts. Giving up.");
        }
    }
}
