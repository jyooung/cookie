package com.sprata.btnpj.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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

    private Set<String> processedHashes = new HashSet<>();

    public void extractHistoryToFile() throws SQLException, IOException {
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        try {
            Thread.sleep(500); // 500ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 기존 데이터 제거 및 초기화
        removeDuplicatesFromFile(OUTPUT_FILE_PATH);

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
                    Date visitDate = new Date(epochTimeMillis);

                    String record = sdf.format(visitDate) + " - " + url;

                    // 중복 데이터가 아닌 경우만 기록
                    if (!isRecordAlreadyProcessed(record)) {
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

        // 기존 데이터 제거 및 초기화
        removeDuplicatesFromFile(YOUTUBE_OUTPUT_FILE_PATH);

        try (BufferedReader youtubeReader = new BufferedReader(new FileReader(YOUTUBE_OUTPUT_FILE_PATH))) {
            String line;
            while ((line = youtubeReader.readLine()) != null) {
                System.out.println("Processing line: " + line);

                String url = extractUrlFromLine(line);
                if (url != null) {
                    System.out.println("Extracted URL: " + url);
                    // 비디오 세부 정보를 추출하여 파일에 저장
                    extractYouTubeVideoDetails(url, line.substring(0, 19)); // 날짜와 함께 전달
                } else {
                    System.out.println("No valid URL found in line.");
                }
            }

            System.out.println("YouTube URLs have been successfully processed.");

        } catch (IOException e) {
            System.out.println("Error reading YouTube history: " + e.getMessage());
            e.printStackTrace();
        }

        // youtube_details.txt 파일을 날짜 순으로 정렬 및 출력
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

            // 데이터가 존재하는 경우 파일에 저장 (날짜 포함)
            if (!title.isEmpty() && !thumbnail.isEmpty()) {
                String details = String.format("Date: %s%nTitle: %s%nThumbnail: %s%nCategories: %s%nTags: %s%n", date, title, thumbnail, categories, tags);
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
                    System.out.println("yt-dlp process exited with non-zero code: " + exitCode);
                }
            } catch (InterruptedException e) {
                System.out.println("yt-dlp process interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isRecordAlreadyProcessed(String record) {
        String hash = hashUrl(record);
        if (processedHashes.contains(hash)) {
            return true;
        }
        processedHashes.add(hash);
        return false;
    }

    private String hashUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    private void removeDuplicatesFromFile(String filePath) throws IOException {
        File inputFile = new File(filePath);
        File tempFile = new File(filePath + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            Set<String> lines = new LinkedHashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            for (String uniqueLine : lines) {
                writer.write(uniqueLine);
                writer.newLine();
            }
        }

        if (!inputFile.delete()) {
            throw new IOException("Failed to delete original file: " + inputFile.getAbsolutePath());
        }
        if (!tempFile.renameTo(inputFile)) {
            throw new IOException("Failed to rename temp file to original file: " + tempFile.getAbsolutePath());
        }
    }

    private String extractUrlFromLine(String line) {
        String[] parts = line.split(" - ");
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }

    private void sortAndPrintYouTubeDetailsByDate() throws IOException {
        File file = new File(YOUTUBE_DETAILS_FILE_PATH);
        if (!file.exists()) {
            System.out.println("Warning: The YouTube details file does not exist.");
            return;
        }

        List<String> lines = Files.readAllLines(Paths.get(YOUTUBE_DETAILS_FILE_PATH));
        lines.sort((line1, line2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date1 = line1.split("\n")[0].substring(6);
                String date2 = line2.split("\n")[0].substring(6);
                Date d1 = (Date) sdf.parse(date1);
                Date d2 = (Date) sdf.parse(date2);
                return d2.compareTo(d1);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(YOUTUBE_DETAILS_FILE_PATH))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        System.out.println("YouTube details sorted by date.");
    }

    private void copyDatabase(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
}
