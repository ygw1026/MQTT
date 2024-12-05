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
    public void writeToInfluxDB(String[] tagName, double value) {
        try {
            Point point = Point.measurement("mqtt_measurement")
                    .addField("sensorValue", value)
                    .addTag("location", tagName[0])
                    .addTag("kind", tagName[1])
                    .addTag("data", tagName[2])
                    .time(System.currentTimeMillis(), WritePrecision.MS);

            WriteApiBlocking writeApi = influxclient.getWriteApiBlocking();
            writeApi.writePoint(bucket, org, point);
            logger.info("Data written to InfluxDB with value: {}", value);
        } catch (Exception e) {
            logger.info("Error writing data to InfluxDB: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
