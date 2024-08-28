package com.sprata.btnpj.service;

import org.springframework.stereotype.Service;

import java.io.*;
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

    /**
     * Chrome 브라우저 히스토리를 파일로 추출합니다.
     * SQLite 데이터베이스에서 히스토리를 읽어와 `chrome_history.txt`와 `chrome_youtube_history.txt` 파일에 기록합니다.
     * YouTube URL은 별도로 `chrome_youtube_history.txt`에 저장됩니다.
     */
    public void extractHistoryToFile() throws SQLException, IOException {
        // 원본 데이터베이스 파일을 복사합니다.
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
                existingRecords.add(line); // 이미 기록된 히스토리를 중복되지 않도록 저장
            }
        } catch (FileNotFoundException e) {
            // 파일이 존재하지 않는 경우 무시
        }

        Connection conn = null;
        try {
            // SQLite 데이터베이스에 연결
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA busy_timeout = 5000;"); // 5000 밀리초 = 5초

            System.out.println("Connection to SQLite has been established.");

            // URL과 방문 시간을 조회하여 최신 방문 순으로 정렬
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
                        // 새로운 기록을 파일에 추가
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

    /**
     * YouTube URL을 추출하여 콘솔에 출력합니다.
     * `chrome_youtube_history.txt` 파일에서 각 URL을 추출하여 콘솔에 표시합니다.
     */
    public void extractYouTubeUrls() throws IOException {
        System.out.println("extractYouTubeUrls 메소드가 실행됐습니다.");

        // YouTube 히스토리 파일의 존재 여부와 비어 있는지 확인
        if (!Files.exists(Paths.get(YOUTUBE_OUTPUT_FILE_PATH))) {
            System.out.println("Warning: The YouTube history file does not exist.");
            return; // 파일이 없으면 메서드 종료
        }

        if (Files.size(Paths.get(YOUTUBE_OUTPUT_FILE_PATH)) == 0) {
            System.out.println("Warning: The YouTube history file is empty.");
            return; // 파일이 비어 있으면 메서드 종료
        }

        try (BufferedReader youtubeReader = new BufferedReader(new FileReader(YOUTUBE_OUTPUT_FILE_PATH))) {

            String line;
            while ((line = youtubeReader.readLine()) != null) {
                System.out.println("Processing line: " + line);  // 디버깅 출력

                String url = extractUrlFromLine(line);
                if (url != null) {
                    System.out.println("Extracted URL: " + url);  // 추출된 URL 콘솔에 출력
                } else {
                    System.out.println("No valid URL found in line.");  // URL 추출 실패 시 출력
                }
            }

            System.out.println("YouTube URLs have been successfully processed.");

        } catch (IOException e) {
            System.out.println("Error reading YouTube history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 원본 데이터베이스 파일을 복사합니다.
     *
     * @param sourceFile 원본 파일
     * @param destFile   복사될 파일
     * @throws IOException 파일 입출력 예외
     */
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

    /**
     * 텍스트 라인에서 URL을 추출합니다.
     *
     * @param line 텍스트 라인
     * @return 추출된 URL 또는 null
     */
    private String extractUrlFromLine(String line) {
        int urlStart = line.indexOf("https://");
        if (urlStart != -1) {
            return line.substring(urlStart);
        }

        return null;
    }
}
