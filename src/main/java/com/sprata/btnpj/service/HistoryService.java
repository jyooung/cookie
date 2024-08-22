package com.sprata.btnpj.service;

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
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class HistoryService {

    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;
    private static final String OUTPUT_FILE_PATH = "chrome_history.txt";
    private static final String YOUTUBE_OUTPUT_FILE_PATH = "chrome_youtube_history.txt";
    private static final String YOUTUBE_DETAILS_OUTPUT_FILE_PATH = "youtube_details.txt";



    public void extractHistoryToFile() throws SQLException, IOException {
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        try {
            Thread.sleep(500); // 500ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<String> existingRecords = new HashSet<>();
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
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA busy_timeout = 5000;"); // 5000 밀리초 = 5초

            System.out.println("Connection to SQLite has been established.");

            ResultSet rs = stmt.executeQuery("SELECT urls.url, visits.visit_time "
                    + "FROM urls INNER JOIN visits ON urls.id = visits.url "
                    + "ORDER BY visits.visit_time DESC");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true));
                 BufferedWriter youtubeWriter = new BufferedWriter(new FileWriter(YOUTUBE_OUTPUT_FILE_PATH, true))) {

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while (rs.next()) {
                    String url = rs.getString("url");
                    long visitTimeMicroseconds = rs.getLong("visit_time");
                    long visitTimeMillis = visitTimeMicroseconds / 1000L;
                    long epochTimeMillis = visitTimeMillis - 11644473600000L;
                    java.util.Date visitDate = new java.util.Date(epochTimeMillis);

                    String record = sdf.format(visitDate) + " - " + url;

                    if (!existingRecords.contains(record)) {
                        writer.write(record);
                        writer.newLine();

                        if (url.contains("youtube.com")) {
                            youtubeWriter.write(record);
                            youtubeWriter.newLine();
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

    public void extractYouTubeDetails() throws IOException {
        // 파일 존재 여부 및 비어 있는지 확인
        if (!Files.exists(Paths.get(YOUTUBE_OUTPUT_FILE_PATH))) {
            System.out.println("Warning: The YouTube history file does not exist.");
            return; // 파일이 없으면 메서드 종료
        }

        if (Files.size(Paths.get(YOUTUBE_OUTPUT_FILE_PATH)) == 0) {
            System.out.println("Warning: The YouTube history file is empty.");
            return; // 파일이 비어 있으면 메서드 종료
        }

        try (BufferedReader youtubeReader = new BufferedReader(new FileReader(YOUTUBE_OUTPUT_FILE_PATH));
             BufferedWriter detailsWriter = new BufferedWriter(new FileWriter(YOUTUBE_DETAILS_OUTPUT_FILE_PATH, true))) {

            String line;
            while ((line = youtubeReader.readLine()) != null) {
                System.out.println("Processing line: " + line);  // 디버깅 출력

                String url = extractUrlFromLine(line);
                if (url != null) {
                    System.out.println("Extracted URL: " + url);  // 디버깅 출력

                    String videoId = getYouTubeVideoId(url);
                    if (videoId != null) {
                        System.out.println("Processing video ID: " + videoId);  // 디버깅 출력

                        VideoInfo videoInfo = fetchYouTubeVideoInfo(videoId);
                        if (videoInfo != null) {
                            String youtubeRecord = "URL: " + url + " - Title: " + videoInfo.getTitle() + " - Thumbnail: " + videoInfo.getThumbnailUrl();
                            detailsWriter.write(youtubeRecord);
                            detailsWriter.newLine();
                            System.out.println("Recorded YouTube video info: " + youtubeRecord);  // 디버깅 출력
                        } else {
                            System.out.println("Video info could not be retrieved for URL: " + url);
                            detailsWriter.write(line + " - No video info available");
                            detailsWriter.newLine();
                        }
                    } else {
                        System.out.println("Could not extract video ID from URL: " + url);
                        detailsWriter.write(line + " - Invalid YouTube URL");
                        detailsWriter.newLine();
                    }
                }
            }

            System.out.println("YouTube details have been successfully written to the file.");

        } catch (IOException e) {
            System.out.println("Error reading YouTube history or writing details: " + e.getMessage());
            e.printStackTrace();
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

    private String extractUrlFromLine(String line) {
        int urlStart = line.indexOf("https://");
        if (urlStart != -1) {
            return line.substring(urlStart);
        }

        return null;
    }

    private String getYouTubeVideoId(String url) {
        String videoId = null;

        // Check for short YouTube URL (youtu.be/VIDEO_ID)
        if (url.contains("youtu.be/")) {
            videoId = url.split("youtu.be/")[1].split("[?&]")[0];
        }
        // Check for standard YouTube URL (youtube.com/watch?v=VIDEO_ID)
        else if (url.contains("youtube.com/watch")) {
            String[] queryParams = url.split("[?&]");
            for (String param : queryParams) {
                if (param.startsWith("v=")) {
                    videoId = param.split("=")[1];
                    break;
                }
            }
        }

        // Log extracted video ID or error message
        if (videoId != null) {
            System.out.println("Extracted Video ID: " + videoId);
        } else {
            System.out.println("No valid Video ID found in URL: " + url);
        }

        return videoId;
    }



    private VideoInfo fetchYouTubeVideoInfo(String videoId) {
        HttpURLConnection conn = null;
        try {
            String urlString = "https://www.youtube.com/oembed?url=" +
                    java.net.URLEncoder.encode("https://www.youtube.com/watch?v=" + videoId, "UTF-8") +
                    "&format=json";
            System.out.println("Fetching video info from: " + urlString);

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("HttpResponseCode: " + responseCode);
                return null;
            }

            StringBuilder inline = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    inline.append(line);
                }
            }

            JSONObject json = new JSONObject(inline.toString());
            String title = json.getString("title");
            String thumbnailUrl = json.getString("thumbnail_url");

            return new VideoInfo(title, thumbnailUrl);

        } catch (IOException | JSONException e) {
            System.out.println("Error fetching video info: " + e.getMessage());
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

