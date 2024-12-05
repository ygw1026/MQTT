package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

public class InfluxDBService {
    static final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);

    private InfluxDBClient influxclient;
    private String bucket = "fromjava";
    private String org = "group2";
    private String token = "S6n6_hcL-H8NBDBH6jT_sSbwC771FcobzRgJ1m8yL8Rs6GFahJZ--FiOnD0sOBPENPnNtoAAIzm0pjFmoF7Dxw==";

    public InfluxDBService() {
        this.influxclient = InfluxDBClientFactory.create("http://192.168.71.218:8086", token.toCharArray(), org, bucket);
    }

    // InfluxDB에 데이터를 기록하는 메서드
    public void writeToInfluxDB(String topic, double value) {
        try {
            // 토픽을 '/' 기준으로 split
            String[] topicParts = topic.split("/");

            // InfluxDB 의 tag값이 될 데이터를 추출
            String location = getTagValueFromTopic(topicParts, "p");
            String kind = getTagValueFromTopic(topicParts, "n");
            String data = getTagValueFromTopic(topicParts, "e");

            // 로그로 출력하여 값이 제대로 추출되었는지 확인
            logger.info("Extracted tagkey -> location: {}, kind: {}, data: {}\nValue into InfluxDB: {}", location, kind, data, value);

            Point point = Point.measurement("mqtt_measurement")
                    .addField("sensorValue", value)
                    .addTag("location", location)
                    .addTag("kind", kind)
                    .addTag("data", data)
                    .time(System.currentTimeMillis(), WritePrecision.MS);

            WriteApiBlocking writeApi = influxclient.getWriteApiBlocking();
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            logger.info("Error writing data to InfluxDB: {}", e.getMessage());
        }
    }

    // 토픽에서 특정 부분만 추출하는 메서드
    private String getTagValueFromTopic(String[] topicParts, String key) {
        for (int i = 0; i < topicParts.length; i++) {
            if (topicParts[i].equals(key)) {
                // 'key' 뒤의 값을 반환
                if (i + 1 < topicParts.length) {
                    return topicParts[i + 1];
                }
            }
        }
        return ""; // key가 없으면 빈 문자열 반환
    }
}
