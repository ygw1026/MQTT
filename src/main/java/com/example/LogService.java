package com.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class LogService {
    private String logFolderPath;
    private String currentHour = "";
    private FileWriter logFileWriter;

    public LogService() {
        loadConfig();
    }

    // config.properties 파일을 로드하여 log.folder.path 설정 값을 읽어옵니다.
    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {  // resources 폴더에서 읽기
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            properties.load(input);  // config.properties 파일 로드
            logFolderPath = properties.getProperty("log.folder.path", "/default/path"); // 기본 경로 설정

            // 경로 확인 로그
            System.out.println("Log Folder Path: " + logFolderPath);  // 경로가 제대로 읽혔는지 확인

            // 경로가 존재하지 않으면 생성
            File folder = new File(logFolderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (created) {
                    System.out.println("Log folder created at: " + logFolderPath);
                } else {
                    System.out.println("Failed to create log folder at: " + logFolderPath);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startLogFileRotation() {
        createNewLogFile();

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String hour = new SimpleDateFormat("HH").format(System.currentTimeMillis());
                if (!hour.equals(currentHour)) {
                    currentHour = hour;
                    createNewLogFile();
                }
            }
        }, 0, 60 * 1000); // 1분마다 체크하여 정각이 되었는지 확인
    }

    public void createNewLogFile() {
        String hour = new SimpleDateFormat("HH").format(System.currentTimeMillis());
        if (!hour.equals(currentHour)) {
            currentHour = hour;
            String logFilePath = logFolderPath + "/log_" + new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis()) + "_" + currentHour + ".txt";
            File logFile = new File(logFilePath);
            try {
                if (!logFile.exists()) {
                    logFile.getParentFile().mkdirs(); // 폴더가 없으면 생성
                    logFile.createNewFile(); // 새 파일 생성
                }
                logFileWriter = new FileWriter(logFile, true);
            } catch (IOException e) {
                System.out.println("Error creating log file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void writeLogToFile(String message) {
        try {
            if (logFileWriter == null) {
                System.out.println("Log file writer is not initialized. Initializing now...");
                createNewLogFile(); // logFileWriter가 null이면 초기화 시도
            }

            if (logFileWriter != null) {
                long timestamp = System.currentTimeMillis();  // 현재 시간 (timestamp)

                // Timestamp를 "yyyy-MM-dd HH:mm:ss" 형식으로 변환
                String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));

                // 로그에 시간 정보 추가
                logFileWriter.append(formattedTime).append(" - ").append(message).append("\n");
                logFileWriter.flush();
            } else {
                System.out.println("Log file writer initialization failed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}