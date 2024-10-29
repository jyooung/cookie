// 모든 데이터 성공적으로 가져와짐
// 현재 실행했을때 크롬방문기록 잘 가져와지고 그 중 유튜브 기록 잘 필터링되고 유튜브 url관련된 세부 정보들 다 잘 추출된다.
// 데이터 추출은 여기서 마무리~
// 현재시간과 충돌해서 에러 발생 현재

package com.sprata.btnpj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Service
public class HistoryService {

    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;
    private static final String OUTPUT_FILE_PATH = "chrome_history5.txt";
    private static final String YOUTUBE_OUTPUT_FILE_PATH = "chrome_youtube_history5.txt";
    private static final String YOUTUBE_DETAILS_FILE_PATH = "youtube_details6.txt";

    private Map<String, Set<Long>> processedUrlsMap = new HashMap<>();
    // 마지막 방문 시간을 저장하기 위한 변수
    private long lastVisitTime = 0;


    public void extractHistoryToFile() throws SQLException, IOException {
        // 1. 원본 DB를 복사하여 안전한 읽기 작업을 보장.
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        // 2. SQLite JDBC 드라이버 로드 및 데이터베이스 연결
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA busy_timeout = 5000;"); // 5000 밀리초 = 5초

            System.out.println("Connection to SQLite has been established.");

            // 3. 기존 기록 불러오기 및 마지막 방문 시간 갱신
            Set<String> existingRecords = new HashSet<>();  // 중복 방지를 위한 Set
            lastVisitTime = loadExistingRecords(existingRecords);  // 기존 기록을 로드하고 마지막 방문 시간 반환

            // 4. SQL 쿼리 실행: 마지막 방문 이후의 기록만 가져오고 오름차순 정렬
            String query = "SELECT urls.url, visits.visit_time " +
                    "FROM urls INNER JOIN visits ON urls.id = visits.url " +
                    "WHERE visits.visit_time > " + lastVisitTime + " " + // 신규 데이터만 선택
                    "ORDER BY visits.visit_time ASC";

            ResultSet rs = stmt.executeQuery(query);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 5. 파일에 기록하기 위한 BufferedWriter 초기화
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true)); // 기존 파일에 추가
                 BufferedWriter youtubeWriter = new BufferedWriter(new FileWriter(YOUTUBE_OUTPUT_FILE_PATH, true))) {

                // 6. ResultSet 순회하며 각 방문기록 처리
                while (rs.next()) {
                    String url = rs.getString("url");
                    long visitTimeMicroseconds = rs.getLong("visit_time");

                    // 마이크로초를 밀리초로 변환 및 Unix epoch 기준으로 변환
                    long visitTimeMillis = visitTimeMicroseconds / 1000L;
                    long epochTimeMillis = visitTimeMillis - 11644473600000L;

                    // Date 객체로 변환
                    java.util.Date visitDate = new java.util.Date(epochTimeMillis);
                    String record = sdf.format(visitDate) + " - " + url;

                    // 7. 중복 기록이 아닌 경우에만 파일에 기록
                    if (!existingRecords.contains(record)) {
                        writer.write(record);
                        writer.newLine();

                        // 유튜브 링크만 필터링하여 다른 파일에 기록
                        if (url.contains("https://www.youtube.com") || url.contains("youtube.com")) {
                            youtubeWriter.write(record);
                            youtubeWriter.newLine();
                        }

                        // 8. 마지막 방문 시간을 갱신
                        lastVisitTime = Math.max(lastVisitTime, visitTimeMillis);
                    }
                }
                System.out.println("Data has been successfully written to the file.");
            } catch (IOException e) {
                System.out.println("Error writing to the file: " + e.getMessage());
            }

        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found.");
        } catch (SQLException e) {
            System.out.println("Error exporting history: " + e.getMessage());
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
        // 해시맵 상태 출력
        printProcessedUrlsMap();
    }

    // 기존 방문 기록을 로드하고 마지막 방문 시간을 반환하는 메서드
    private long loadExistingRecords(Set<String> existingRecords) {
        long lastVisitTime = 0; // 마지막 방문 시간을 저장할 변수 초기화
        List<String> uniqueRecords = new ArrayList<>(); // 중복 제거를 위한 List 생성

        // chrome_history5.txt 파일을 읽어 기존 방문 기록을 로드
        try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_FILE_PATH))) {
            String line; // 파일의 각 줄을 읽기 위한 변수
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 날짜 형식 지정

            // 파일의 각 줄을 한 줄씩 읽음
            while ((line = reader.readLine()) != null) {
                // 현재 줄의 형식: "YYYY-MM-DD HH:MM:SS - URL"
                // " - " 구분자로 방문 시간과 URL을 분리
                String[] parts = line.split(" - ");
                if (parts.length == 2) { // 올바른 형식인지 확인
                    String recordKey = parts[0] + " - " + parts[1]; // 유일한 키 생성

                    // 기존 방문 기록에 추가
                    if (!existingRecords.contains(recordKey)) {
                        uniqueRecords.add(recordKey); // 중복되지 않은 기록만 추가
                        existingRecords.add(recordKey); // 중복 방지 Set에 추가
                    }
                }

                // 마지막 방문 시간을 업데이트
                int delimiterIndex = line.indexOf(" - "); // " - " 구분자의 인덱스
                if (delimiterIndex != -1) { // 구분자가 존재하는지 확인
                    String dateString = line.substring(0, delimiterIndex); // 날짜 부분 추출
                    Date visitDate = sdf.parse(dateString); // 문자열을 Date 객체로 변환
                    lastVisitTime = Math.max(lastVisitTime, visitDate.getTime()); // 마지막 방문 시간을 업데이트
                }
            }
        } catch (FileNotFoundException e) {
            // 파일이 존재하지 않을 경우 아무 작업도 하지 않음 (초기 실행)
            System.out.println("기존 방문기록 파일이 없습니다. 새로 생성됩니다.");
        } catch (IOException | java.text.ParseException e) {
            // 입출력 오류나 날짜 파싱 오류 처리
            System.out.println("기존 기록 로드 오류: " + e.getMessage());
        }

        // 중복 제거 후 유일한 기록을 오름차순으로 정렬하여 파일에 다시 쓰기
        writeUniqueRecordsToFile(uniqueRecords); // 중복이 제거된 유일한 기록을 파일에 저장
        return lastVisitTime; // 마지막 방문 시간 반환
    }

    // 유일한 기록을 chrome_history5.txt 파일에 쓰는 메서드
    private void writeUniqueRecordsToFile(List<String> uniqueRecords) {
        // 유일한 기록을 오름차순으로 정렬
        uniqueRecords.sort((a, b) -> {
            String dateA = a.split(" - ")[0]; // 첫 번째 기록의 날짜 부분
            String dateB = b.split(" - ")[0]; // 두 번째 기록의 날짜 부분
            return dateA.compareTo(dateB); // 날짜 순서 비교
        });

        // 유일한 기록을 파일에 저장
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH))) {
            for (String record : uniqueRecords) { // 중복이 제거된 각 기록을 순회
                writer.write(record); // 기록을 파일에 작성
                writer.newLine(); // 다음 줄로 이동
            }
            System.out.println("중복이 제거된 방문기록이 날짜 순서로 저장되었습니다."); // 완료 메시지 출력
        } catch (IOException e) { // 파일 쓰기 오류 처리
            System.out.println("파일 쓰기 오류: " + e.getMessage());
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


    // Async로 YouTube URL 추출 작업 수행
    @Async
    public CompletableFuture<Void> extractYouTubeUrls() throws IOException {
        System.out.println("extractYouTubeUrls 메소드가 실행됐습니다.");

        // youtube_details.txt에서 마지막 처리된 날짜 확인
        printLastProcessedDetails();

        // YouTube 기록 파일이 존재하는지 확인
        if (!Files.exists(Paths.get(YOUTUBE_OUTPUT_FILE_PATH))) {
            System.out.println("Warning: The YouTube history file does not exist.");
            return CompletableFuture.completedFuture(null);
        }

        // YouTube 기록 파일이 비어 있는지 확인
        if (Files.size(Paths.get(YOUTUBE_OUTPUT_FILE_PATH)) == 0) {
            System.out.println("Warning: The YouTube history file is empty.");
            return CompletableFuture.completedFuture(null);
        }

        // 마지막으로 처리된 날짜 가져오기
        String lastProcessedDate = getLastProcessedDate();
        if (lastProcessedDate == null) {
            System.out.println("No last processed date found. Processing all YouTube URLs.");
        } else {
            System.out.println("Last processed date: " + lastProcessedDate);
        }

        // 중복 제거 후 YouTube URL 파일 읽기
        removeDuplicatesFromFile(YOUTUBE_OUTPUT_FILE_PATH);

        try (BufferedReader youtubeReader = new BufferedReader(new FileReader(YOUTUBE_OUTPUT_FILE_PATH))) {
            String line;
            while ((line = youtubeReader.readLine()) != null) {
                System.out.println("Processing line: " + line);

                // URL과 방문 날짜 추출
                String url = extractUrlFromLine(line);
                String date = line.substring(0, 19);

                // 마지막 처리된 날짜 이후의 URL만 처리
                if (url != null && (lastProcessedDate == null || date.compareTo(lastProcessedDate) > 0)) {
                    System.out.println("Extracting details for URL: " + url + " visited on: " + date);
                    extractYouTubeVideoDetails(url, date);
                } else {
                    System.out.println("Skipping URL: " + url + " visited on: " + date);
                }
            }
        }

        // YouTube 세부 정보 파일 정렬 및 출력
        sortAndPrintYouTubeDetailsByDate();

        return CompletableFuture.completedFuture(null);
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
                Date d1 = sdf.parse(date1);
                Date d2 = sdf.parse(date2);
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

    // 날짜 형식을 검증하는 메서드
    private boolean isValidDateFormat(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setLenient(false); // 엄격한 날짜 형식 검증
        try {
            sdf.parse(dateString); // 날짜 형식 검증 시도
            return true;
        } catch (ParseException e) {
            return false; // 유효하지 않은 경우 false 반환
        }
    }

    private void extractYouTubeVideoDetails(String url, String date) throws IOException {
        // 날짜 형식 검증
        // 입력된 날짜가 유효한 형식인지 확인. 유효하지 않다면 URL을 건너뜁니다.
        if (!isValidDateFormat(date)) {
            System.out.println("Skipping URL due to invalid date format: " + date + " for URL: " + url);
            return;
        }

        // URL이 비디오 페이지를 가리키는지 확인
        // YouTube의 비디오 URL은 "watch?v=" 형식을 포함하므로 이를 통해 비디오 URL을 판별합니다.
        if (!url.contains("watch?v=")) {
            System.out.println("Skipping non-video URL: " + url);
            return;
        }

        // yt-dlp 프로세스 실행
        // yt-dlp는 YouTube URL에서 메타 데이터를 추출하는 외부 프로그램입니다.
        // ProcessBuilder를 통해 yt-dlp를 호출하며, -j 옵션은 JSON 형식으로 결과를 출력하도록 합니다.
        ProcessBuilder processBuilder = new ProcessBuilder("yt-dlp", "-j", url);

        Process process;
        try {
            // yt-dlp 프로세스를 시작합니다.
            process = processBuilder.start();
        } catch (IOException e) {
            // 프로세스 시작 중 예외가 발생할 경우 에러 메시지를 출력하고 메서드를 종료합니다.
            System.out.println("Error starting yt-dlp process: " + e.getMessage());
            return;
        }

        // yt-dlp 출력 결과와 파일 작성을 위한 리더와 라이터 초기화
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new FileWriter(YOUTUBE_DETAILS_FILE_PATH, true))) {

            // yt-dlp의 JSON 출력 결과를 저장할 StringBuilder 초기화
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // yt-dlp의 각 라인을 읽어와 jsonOutput에 추가
                jsonOutput.append(line);
            }

            // JSON 데이터가 수신되었는지 확인
            // yt-dlp의 출력이 없다면 데이터를 수신하지 못한 것으로 간주하고 메서드를 종료합니다.
            if (jsonOutput.length() == 0) {
                System.out.println("No data received from yt-dlp for URL: " + url);
                return;
            }

            // JSON 데이터를 파싱하기 위해 ObjectMapper 객체 사용
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode;
            try {
                // jsonOutput을 JSON 형식으로 파싱하여 JsonNode 객체 생성
                jsonNode = objectMapper.readTree(jsonOutput.toString());
            } catch (IOException e) {
                // JSON 파싱 실패 시 에러 메시지를 출력하고 메서드를 종료
                System.out.println("Error parsing JSON output: " + e.getMessage());
                return;
            }

            // 필요한 데이터 추출
            // 추출할 필드: 제목(title), 썸네일(thumbnail), 카테고리(categories), 태그(tags)
            String title = jsonNode.path("title").asText();            // 제목 추출
            String thumbnail = jsonNode.path("thumbnail").asText();    // 썸네일 URL 추출
            String categories = jsonNode.path("categories").toString(); // 카테고리 리스트 추출
            String tags = jsonNode.path("tags").toString();            // 태그 리스트 추출

            // 디버깅용: 추출된 데이터가 올바르게 가져왔는지 콘솔에 출력
            System.out.println("Extracted Title: " + title);
            System.out.println("Extracted Thumbnail: " + thumbnail);
            System.out.println("Extracted Categories: " + categories);
            System.out.println("Extracted Tags: " + tags);

            // 데이터가 존재하는 경우 파일에 저장 (날짜 및 URL 포함)
            // 제목과 썸네일이 빈 값이 아닌 경우만 파일에 저장
            if (!title.isEmpty() && !thumbnail.isEmpty()) {
                // 저장할 세부 데이터 형식 지정 (날짜, URL, 제목, 썸네일, 카테고리, 태그 포함)
                String details = String.format("Date: %s%nURL: %s%nTitle: %s%nThumbnail: %s%nCategories: %s%nTags: %s%n",
                        date, url, title, thumbnail, categories, tags);
                // 지정한 형식으로 파일에 쓰기
                writer.write(details);
                writer.newLine();  // 새 라인 추가
            } else {
                // 데이터가 불완전할 경우 저장하지 않고 건너뜁니다.
                System.out.println("Skipping saving because the data is incomplete.");
            }

        } catch (IOException e) {
            // yt-dlp 프로세스의 출력 읽기 오류가 발생할 경우 에러 메시지와 스택 추적 출력
            System.out.println("Error reading from yt-dlp process: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // yt-dlp 프로세스의 종료 코드 확인
                // 정상 종료가 아닌 경우 비정상 종료 메시지를 출력
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.out.println("yt-dlp process exited with non-zero code: " + exitCode);
                }
            } catch (InterruptedException e) {
                // 프로세스 대기 중 인터럽트가 발생할 경우 메시지를 출력하고 현재 스레드에 인터럽트 플래그 설정
                System.out.println("yt-dlp process interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }



    //
    private String extractUrlFromLine(String line) {
        String[] parts = line.split(" - ");
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }

    // youtube_details.txt에서 마지막으로 추출한 세부 정보를 출력하는 메소드 추가
    private void printLastProcessedDetails() throws IOException {
        File detailsFile = new File(YOUTUBE_DETAILS_FILE_PATH);
        if (!detailsFile.exists() || detailsFile.length() == 0) {
            System.out.println("youtube_details.txt 파일이 존재하지 않거나 비어 있습니다.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(detailsFile))) {
            String lastLine = null;
            String line;
            StringBuilder lastDetails = new StringBuilder();

            // 마지막으로 저장된 비디오 세부 정보를 읽음
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Date: ")) {
                    lastDetails.setLength(0);  // 새로운 세부 정보 블록이 시작될 때 초기화
                }
                lastDetails.append(line).append("\n");
                lastLine = line;  // 마지막 줄을 계속 갱신
            }

            // 마지막 세부 정보를 콘솔에 출력
            if (lastDetails.length() > 0) {
                System.out.println("마지막으로 처리된 비디오 세부 정보:");
                System.out.println(lastDetails.toString());
            } else {
                System.out.println("마지막 비디오 세부 정보를 찾을 수 없습니다.");
            }
        }
    }

    // 현재까지 처리된 URL과 방문 시간을 확인하고 출력하는 메서드 (디버깅 용도)
    //HashMap 데이터 저장 확인하기
    private void printProcessedUrlsMap() {
        System.out.println("Current state of processedUrlsMap:");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, Set<Long>> entry : processedUrlsMap.entrySet()) {
            String url = entry.getKey();
            Set<Long> visitTimes = entry.getValue();

            System.out.println("URL: " + url);
            System.out.println("Visit Times:");

            for (Long visitTime : visitTimes) {
                Date date = convertMicrosecondsToDate(visitTime);
                String formattedDate = sdf.format(date);
                System.out.println(" - " + formattedDate);
            }

            System.out.println(); // 빈 줄 추가
        }
    }
    //마이크로초를 Date 객체로 변환하는 메서드
    private Date convertMicrosecondsToDate(long microseconds) {
        long millis = microseconds / 1000L;
        long epochMillis = millis - 11644473600000L; // Windows epoch time 보정
        return new Date(epochMillis);
    }

    private String getLastProcessedDate() throws IOException {
        File detailsFile = new File(YOUTUBE_DETAILS_FILE_PATH);
        if (!detailsFile.exists() || detailsFile.length() == 0) {
            return null;
        }

        try (RandomAccessFile file = new RandomAccessFile(detailsFile, "r")) {
            long fileLength = file.length();
            long pointer = fileLength - 1;
            StringBuilder lineBuilder = new StringBuilder();
            String lastDate = null;
            boolean foundDate = false;

            // 파일 끝에서부터 역방향으로 읽기
            while (pointer >= 0) {
                file.seek(pointer);
                int readByte = file.readByte();
                if (readByte == '\n') {
                    String line = lineBuilder.reverse().toString().trim();
                    lineBuilder.setLength(0); // 줄 처리 후 StringBuilder 초기화

                    // 'Date: '로 시작하는 줄을 찾으면 그 날짜를 추출
                    if (line.startsWith("Date: ")) {
                        lastDate = line.substring(6, 25); // 'Date: ' 이후의 날짜 (yyyy-MM-dd HH:mm:ss) 추출
                        foundDate = true;
                        break;
                    }
                } else {
                    lineBuilder.append((char) readByte);
                }
                pointer--;
            }

            // 마지막 줄 처리
            if (!foundDate && lineBuilder.length() > 0) {
                String line = lineBuilder.reverse().toString().trim();
                if (line.startsWith("Date: ")) {
                    lastDate = line.substring(6, 25);
                }
            }

            // 마지막 날짜 출력
            if (lastDate != null) {
                System.out.println("추출된 마지막 날짜: " + lastDate);
                return lastDate;
            } else {
                System.out.println("No valid date found in youtube_details.txt.");
            }
        }

        return null;
    }


    // 데이터베이스 파일을 복사하는 메서드
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
