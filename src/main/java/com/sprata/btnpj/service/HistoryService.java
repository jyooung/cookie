package com.sprata.btnpj.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletableFuture;

@Service
public class HistoryService {

    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;
    private static final String OUTPUT_FILE_PATH = "chrome_history.txt";
    private static final String YOUTUBE_OUTPUT_FILE_PATH = "chrome_youtube_history.txt";
    private static final String YOUTUBE_DETAILS_FILE_PATH = "youtube_details.txt";
    // 새롭게 추가된 필드
    private Set<String> processedHashes = new HashSet<>();


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
                    long epochTimeMillis = visitTimeMillis - 11644473600000L; // Windows epoch time 보정
                    java.util.Date visitDate = new java.util.Date(epochTimeMillis);

                    String record = sdf.format(visitDate) + " - " + url;

                    if (!existingRecords.contains(record)) {
                        writer.write(record);
                        writer.newLine();

                        if (url.contains("youtube.com")) {
                            youtubeWriter.write(record);
                            youtubeWriter.newLine(); // YouTube URL은 별도 파일에 기록
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

    @Async
    public CompletableFuture<Void> extractYouTubeUrls() throws IOException {
        System.out.println("extractYouTubeUrls 메소드가 실행됐습니다.");

        if (!Files.exists(Paths.get(YOUTUBE_OUTPUT_FILE_PATH))) {
            System.out.println("Warning: The YouTube history file does not exist.");
            return CompletableFuture.completedFuture(null);
        }

        if (Files.size(Paths.get(YOUTUBE_OUTPUT_FILE_PATH)) == 0) {
            System.out.println("Warning: The YouTube history file is empty.");
            return CompletableFuture.completedFuture(null);
        }

        try (BufferedReader youtubeReader = new BufferedReader(new FileReader(YOUTUBE_OUTPUT_FILE_PATH))) {
            String line;
            while ((line = youtubeReader.readLine()) != null) {
                System.out.println("Processing line: " + line);

                String url = extractUrlFromLine(line);
                if (url != null) {
                    System.out.println("Extracted URL: " + url);
                    // 비디오 세부 정보를 추출하여 파일에 저장
                    extractYouTubeVideoDetails(url);
                } else {
                    System.out.println("No valid URL found in line.");
                }
            }

            System.out.println("YouTube URLs have been successfully processed.");

        } catch (IOException e) {
            System.out.println("Error reading YouTube history: " + e.getMessage());
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * YouTube 비디오의 제목, 썸네일 링크, 카테고리, 비디오 태그를 추출하여 `youtube_details.txt`에 저장합니다.
     *
     * @param url YouTube 비디오 URL
     * @throws IOException 파일 입출력 예외
     */
    private void extractYouTubeVideoDetails(String url) throws IOException {
        // URL이 비디오를 가리키는지 확인
        if (!url.contains("watch?v=")) {
            System.out.println("Skipping non-video URL: " + url);
            return;
        }

        // URL 해시 생성 및 중복 처리 체크
        String urlHash = hashUrl(url);
        if (processedHashes.contains(urlHash)) {
            System.out.println("Skipping already processed URL: " + url);
            return;
        }

        // yt-dlp 프로세스 실행
        ProcessBuilder processBuilder = new ProcessBuilder("yt-dlp", "-j", url);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            System.out.println("Error starting yt-dlp process: " + e.getMessage());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new FileWriter(YOUTUBE_DETAILS_FILE_PATH, true))) {

            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonOutput.append(line);
            }

            // JSON 데이터가 수신되었는지 확인
            if (jsonOutput.length() == 0) {
                System.out.println("No data received from yt-dlp for URL: " + url);
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(jsonOutput.toString());
            } catch (IOException e) {
                System.out.println("Error parsing JSON output: " + e.getMessage());
                return;
            }

            // 필요한 데이터 추출
            String title = jsonNode.path("title").asText();
            String thumbnail = jsonNode.path("thumbnail").asText();
            String categories = jsonNode.path("categories").toString();
            String tags = jsonNode.path("tags").toString();

            // 디버깅: 추출된 데이터를 확인
            System.out.println("Extracted Title: " + title);
            System.out.println("Extracted Thumbnail: " + thumbnail);
            System.out.println("Extracted Categories: " + categories);
            System.out.println("Extracted Tags: " + tags);


            // 데이터가 존재하는 경우 파일에 저장
            if (!title.isEmpty() && !thumbnail.isEmpty()) {
                String details = String.format("Title: %s%nThumbnail: %s%nCategories: %s%nTags: %s%n", title, thumbnail, categories, tags);
                writer.write(details);
                writer.newLine();
                processedHashes.add(urlHash); // URL 해시를 처리된 리스트에 추가

                System.out.println("Video details extracted and saved for URL: " + url);
            } else {
                System.out.println("Skipping saving because the data is incomplete.");
            }

        } catch (IOException e) {
            System.out.println("Error reading from yt-dlp process: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.out.println("yt-dlp process exited with error code: " + exitCode);
                }
            } catch (InterruptedException e) {
                System.out.println("Error waiting for yt-dlp process to complete: " + e.getMessage());
            }
        }
    }

    /**
     * URL을 해싱하여 고유한 식별자를 생성합니다.
     *
     * @param url 해싱할 URL
     * @return URL의 해시값
     */
    private String hashUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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
}
