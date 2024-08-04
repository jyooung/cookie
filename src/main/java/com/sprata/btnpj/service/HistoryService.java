package com.sprata.btnpj.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.text.SimpleDateFormat;

@Service
public class HistoryService {

    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;
    private static final String OUTPUT_FILE_PATH = "chrome_history.txt";
    private static final String YOUTUBE_OUTPUT_FILE_PATH = "chrome_youtube_history.txt";

    public void extractHistoryToFile() throws SQLException, IOException {
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        try {
            Thread.sleep(500); // 500ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Connection conn = null;
        try {
            // SQLite JDBC 드라이버 로드
            Class.forName("org.sqlite.JDBC");

            // 데이터베이스 연결
            conn = DriverManager.getConnection(DB_URL);

            // busy_timeout 설정
            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA busy_timeout = 5000;"); // 5000 밀리초 = 5초

            System.out.println("Connection to SQLite has been established.");

            // SQL 쿼리 실행
            ResultSet rs = stmt.executeQuery("SELECT urls.url, visits.visit_time "
                    + "FROM urls INNER JOIN visits ON urls.id = visits.url "
                    + "ORDER BY visits.visit_time DESC");

            // 파일에 쓰기
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH));
                 BufferedWriter youtubeWriter = new BufferedWriter(new FileWriter(YOUTUBE_OUTPUT_FILE_PATH))) {

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while (rs.next()) {
                    String url = rs.getString("url");
                    long visitTimeMicroseconds = rs.getLong("visit_time");

                    // 마이크로초를 밀리초로 변환
                    long visitTimeMillis = visitTimeMicroseconds / 1000L;

                    // Unix epoch 기준으로 변환
                    long epochTimeMillis = visitTimeMillis - 11644473600000L;

                    // Date 객체로 변환
                    java.util.Date visitDate = new java.util.Date(epochTimeMillis);

                    // 방문 시간과 URL을 파일에 기록
                    String record = sdf.format(visitDate) + " - " + url;
                    writer.write(record);
                    writer.newLine();

                    // 유튜브 링크만 필터링하여 다른 파일에 기록
                    if (url.contains("https://www.youtube.com") || url.contains("youtube.com")) {
                        youtubeWriter.write(record);
                        youtubeWriter.newLine();
                    }
                }
                System.out.println("Data has been successfully written to the files.");
            } catch (IOException e) {
                System.out.println("Error writing to the files: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error exporting history: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void copyDatabase(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
        System.out.println("Database copied successfully.");
    }
}
