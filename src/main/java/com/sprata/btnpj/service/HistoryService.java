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
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Service
public class HistoryService {

    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;
    private static final String OUTPUT_FILE_PATH = "chrome_history.txt";
    private static final String YOUTUBE_OUTPUT_FILE_PATH = "chrome_youtube_history.txt";
    private static final String YOUTUBE_DETAILS_FILE_PATH = "youtube_details.txt";
    private static final String PROCESSED_URLS_FILE_PATH = "processed_urls.txt";
    private static final String LAST_VISIT_TIME_FILE_PATH = "last_visit_time.txt"; // 마지막 기록된 시간 저장

    private Set<String> processedUrls = new HashSet<>();
    private Set<String> existingRecords = new HashSet<>();
    private long lastVisitTime = -1; // 마지막 기록된 방문 시간

    public void extractHistoryToFile() throws SQLException, IOException {
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        try {
            Thread.sleep(500); // 500ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 기존 기록 로드
        existingRecords = loadExistingRecords(OUTPUT_FILE_PATH);

        // 마지막 기록된 방문 시간 로드
        lastVisitTime = loadLastVisitTime();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA busy_timeout = 5000;"); // 5000 밀리초 = 5초

            System.out.println("Connection to SQLite has been established.");

            // 마지막 방문 기록 이후의 데이터만 가져오는 SQL 쿼리
            String query = "SELECT urls.url, visits.visit_time "
                    + "FROM urls INNER JOIN visits ON urls.id = visits.url "
                    + "WHERE visits.visit_time > " + lastVisitTime
                    + " ORDER BY visits.visit_time ASC"; // 최신 방문 기록부터

            ResultSet rs = stmt.executeQuery(query);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true));
                 BufferedWriter youtubeWriter = new BufferedWriter(new FileWriter(YOUTUBE_OUTPUT_FILE_PATH, true))) {

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while (rs.next()) {
                    String url = rs.getString("url");
                    long visitTimeMicroseconds = rs.getLong("visit_time");
                    long visitTimeMillis = visitTimeMicroseconds / 1000L;
                    long epochTimeMillis = visitTimeMillis - 11644473600000L; // Windows epoch time 보정
                    Date visitDate = new Date(epochTimeMillis);

                    String record = sdf.format(visitDate) + " - " + url;

                    if (!existingRecords.contains(record)) {
                        writer.write(record);
                        writer.newLine();

                        if (url.contains("youtube.com")) {
                            youtubeWriter.write(record);
                            youtubeWriter.newLine(); // YouTube URL은 별도 파일에 기록
                        }
                    }

                    // 마지막 방문 시간을 갱신
                    if (visitTimeMicroseconds > lastVisitTime) {
                        lastVisitTime = visitTimeMicroseconds;
                    }
                }

                // 최신 방문 시간 저장
                saveLastVisitTime(lastVisitTime);

                System.out.println("Data has been successfully written to the files.");
            } catch (IOException e) {
                System.out.println("Error writing to the files: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.out.println("Error exporting history: " + e.getMessage());
            e.printStackTrace();
            throw e;
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

        // Load processed URLs from file
        processedUrls = loadExistingRecords(PROCESSED_URLS_FILE_PATH);

        try (BufferedReader youtubeReader = new BufferedReader(new FileReader(YOUTUBE_OUTPUT_FILE_PATH))) {
            String line;
            while ((line = youtubeReader.readLine()) != null) {
                String url = extractUrlFromLine(line);
                if (url != null && !processedUrls.contains(hashUrl(url))) {
                    extractYouTubeVideoDetails(url, line.substring(0, 19)); // 날짜와 함께 전달
                } else {
                    System.out.println("Skipping URL: " + url);
                }
            }

            // After processing, save the processed URLs
            saveProcessedUrls();

            System.out.println("YouTube URLs have been successfully processed.");

        } catch (IOException e) {
            System.out.println("Error reading YouTube history: " + e.getMessage());
            e.printStackTrace();
        }

        // Sort and print YouTube details by date
        sortAndPrintYouTubeDetailsByDate();

        return CompletableFuture.completedFuture(null);
    }

    private void extractYouTubeVideoDetails(String url, String date) throws IOException {
        // URL이 비디오를 가리키는지 확인
        if (!url.contains("watch?v=")) {
            System.out.println("Skipping non-video URL: " + url);
            return;
        }

        // URL 해시 생성 및 중복 처리 체크
        String urlHash = hashUrl(url);
        if (processedUrls.contains(urlHash)) {
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

            // 데이터가 존재하는 경우 파일에 저장 (날짜 포함)
            if (!title.isEmpty() && !thumbnail.isEmpty()) {
                String details = String.format("Date: %s%nTitle: %s%nThumbnail: %s%nCategories: %s%nTags: %s%n", date, title, thumbnail, categories, tags);
                writer.write(details);
                writer.newLine();
                processedUrls.add(urlHash); // URL 해시를 처리된 리스트에 추가

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

    private void saveProcessedUrls() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PROCESSED_URLS_FILE_PATH, true))) {
            for (String urlHash : processedUrls) {
                writer.write(urlHash);
                writer.newLine();
            }
        }
    }

    private Set<String> loadExistingRecords(String filePath) throws IOException {
        Set<String> records = new HashSet<>();
        if (Files.exists(Paths.get(filePath))) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    records.add(line);
                }
            }
        }
        return records;
    }

    private String extractUrlFromLine(String line) {
        // line 형식: 2023-09-04 12:34:56 - http://example.com
        int index = line.indexOf(" - ");
        if (index != -1) {
            return line.substring(index + 3).trim();
        }
        return null;
    }

    private String hashUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found", e);
        }
    }

    private void sortAndPrintYouTubeDetailsByDate() {
        // YouTube 상세 정보 파일을 정렬 및 출력하는 로직
        // 구체적인 구현은 이전 코드에 따라 적절히 추가 필요
    }

    private void copyDatabase(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    private long loadLastVisitTime() throws IOException {
        if (Files.exists(Paths.get(LAST_VISIT_TIME_FILE_PATH))) {
            try (BufferedReader reader = new BufferedReader(new FileReader(LAST_VISIT_TIME_FILE_PATH))) {
                return Long.parseLong(reader.readLine().trim());
            }
        }
        return -1;
    }

    private void saveLastVisitTime(long lastVisitTime) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_VISIT_TIME_FILE_PATH))) {
            writer.write(Long.toString(lastVisitTime));
        }
    }
}
