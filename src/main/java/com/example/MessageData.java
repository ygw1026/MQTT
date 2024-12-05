package com.example;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageData {
    static final Logger logger = LoggerFactory.getLogger(MessageData.class);

    private static InfluxDBService influxDBService;
    private String topic;
    private String message;

    public MessageData() {}

    public MessageData(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // 메시지 수신 처리 메서드
    public void handleMessageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());

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

        } catch (Exception e) {
            logger.error("Error processing message from topic {}: {}", topic, e.getMessage());
            e.printStackTrace();
        }
    }
}
