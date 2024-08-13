package com.sprata.btnpj.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

@Service
public class HistoryService {

    private static final String API_KEY = "YOUR_YOUTUBE_API_KEY"; // YouTube API 키를 여기에 입력하세요.
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

        Set<String> existingRecords = new HashSet<>();
        // 기존 파일에서 기존 기록을 읽어와 Set에 저장
        try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                existingRecords.add(line);
            }
        } catch (FileNotFoundException e) {
            // 파일이 존재하지 않는 경우 무시
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
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true)); // true를 사용하여 파일에 덧붙이기 모드로 열기
                 BufferedWriter youtubeWriter = new BufferedWriter(new FileWriter(YOUTUBE_OUTPUT_FILE_PATH, true))) {

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

                    // 기존 기록에 없는 경우에만 파일에 기록
                    if (!existingRecords.contains(record)) {
                        writer.write(record);
                        writer.newLine();

                        // 유튜브 링크만 필터링하여 다른 파일에 기록
                        if (url.contains("youtube.com")) {
                            String videoId = getYouTubeVideoId(url);
                            if (videoId != null) {
                                VideoInfo videoInfo = fetchYouTubeVideoInfo(videoId);
                                if (videoInfo != null) {
                                    // 제목 또는 썸네일이 없는 경우 예외 처리
                                    String title = videoInfo.getTitle() != null ? videoInfo.getTitle() : "No title available";
                                    String thumbnailUrl = videoInfo.getThumbnailUrl() != null ? videoInfo.getThumbnailUrl() : "No thumbnail available";

                                    String youtubeRecord = record + " - Title: " + title + " - Thumbnail: " + thumbnailUrl;
                                    youtubeWriter.write(youtubeRecord);
                                    youtubeWriter.newLine();
                                } else {
                                    System.out.println("Video info could not be retrieved for URL: " + url);
                                    youtubeWriter.write(record + " - No video info available");
                                    youtubeWriter.newLine();
                                }
                            } else {
                                System.out.println("Could not extract video ID from URL: " + url);
                                youtubeWriter.write(record + " - Invalid YouTube URL");
                                youtubeWriter.newLine();
                            }
                        }
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

    private String getYouTubeVideoId(String url) {
        String[] parts = url.split("v=");
        if (parts.length > 1) {
            return parts[1].split("&")[0];
        }
        return null;
    }

    private VideoInfo fetchYouTubeVideoInfo(String videoId) {
        HttpURLConnection conn = null;
        try {
            String urlString = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId + "&key=" + API_KEY + "&part=snippet";
            System.out.println("Fetching video info from: " + urlString);

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }

            StringBuilder inline = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    inline.append(line);
                }
            }

            // JSON 응답 출력
            System.out.println("JSON Response: " + inline);

            try {
                JSONObject json = new JSONObject(inline.toString());
                JSONArray items = json.getJSONArray("items");
                if (items.length() > 0) {
                    JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
                    String title = snippet.has("title") ? snippet.getString("title") : "No title available";
                    String thumbnailUrl = snippet.has("thumbnails")
                            ? snippet.getJSONObject("thumbnails").has("default")
                            ? snippet.getJSONObject("thumbnails").getJSONObject("default").getString("url")
                            : "No thumbnail available"
                            : "No thumbnail available";

                    // 디버깅 정보 출력
                    System.out.println("Video Title: " + title);
                    System.out.println("Thumbnail URL: " + thumbnailUrl);

                    return new VideoInfo(title, thumbnailUrl);
                } else {
                    System.out.println("No items found in the response.");
                    return null;
                }
            } catch (JSONException e) {
                System.out.println("Error parsing JSON response: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    class VideoInfo {
        private String title;
        private String thumbnailUrl;

        public VideoInfo(String title, String thumbnailUrl) {
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
    }
}
