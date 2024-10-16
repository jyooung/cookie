// 크롬 방문기록 중복없음, 정렬 잘 됨
// 유튜브 기록만 추출하는 것도 잘 되고 있음

package com.sprata.btnpj.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HistoryService {

    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;
    private static final String OUTPUT_FILE_PATH = "chrome_history5.txt";
    private static final String YOUTUBE_OUTPUT_FILE_PATH = "chrome_youtube_history5.txt";

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
