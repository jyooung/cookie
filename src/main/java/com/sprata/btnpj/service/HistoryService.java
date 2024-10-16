package com.sprata.btnpj.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

@Service
public class HistoryService {

    // 원본 크롬 방문기록 DB 경로
    private static final String ORIGINAL_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";

    // 복사된 DB 파일 경로 (SQLite 접근용)
    private static final String COPIED_DB_FILE_PATH = "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";

    // SQLite DB URL 설정
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;

    // 방문기록이 저장될 파일 경로
    private static final String OUTPUT_FILE_PATH = "chrome_history5.txt";

    // 방문기록을 추출하고 파일에 저장하는 메서드
    public void extractHistoryToFile() throws SQLException, IOException {
        // 1. 원본 크롬 DB를 복사하여 안정적인 데이터 접근을 보장
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        // DB 복사 후 안정화를 위해 500ms 대기
        try {
            Thread.sleep(500); // 짧은 지연시간을 통해 파일 접근 문제 예방
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 발생 시 스레드 종료 처리
        }

        // 2. 기존 파일의 방문기록을 불러와 중복 방지용 Set에 저장
        Set<String> existingRecords = new HashSet<>();
        long lastVisitTime = loadExistingRecords(existingRecords); // 마지막 방문 날짜(밀리초) 반환

        // 콘솔에 마지막 방문 날짜 출력 (기존 기록이 있을 경우)
        if (lastVisitTime > 0) {
            String lastVisitDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastVisitTime));
            System.out.println("가장 마지막에 가져온 방문 날짜: " + lastVisitDate);
        } else {
            System.out.println("기존 방문 기록이 없습니다. 모든 기록을 가져옵니다.");
        }

        // 3. DB에서 새로운 방문기록을 조회하여 중복되지 않은 데이터만 수집
        List<String> newRecords = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            System.out.println("SQLite 연결 성공.");

            // SQL 쿼리: 마지막 방문 날짜 이후의 기록만 가져오기 (오름차순 정렬)
            String query = "SELECT urls.url, visits.visit_time " +
                    "FROM urls INNER JOIN visits ON urls.id = visits.url " +
                    "WHERE visits.visit_time / 1000 - 11644473600000 > " + lastVisitTime +
                    " ORDER BY visits.visit_time ASC";

            // 쿼리 실행 결과를 ResultSet으로 받아 처리
            ResultSet rs = stmt.executeQuery(query);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 쿼리 결과를 순회하며 각 방문기록을 처리
            while (rs.next()) {
                String url = rs.getString("url"); // 방문한 URL
                long visitTimeMicroseconds = rs.getLong("visit_time"); // 방문 시간(마이크로초)
                long visitTimeMillis = visitTimeMicroseconds / 1000L - 11644473600000L; // Windows epoch 조정

                // 기록 형식: "YYYY-MM-DD HH:MM:SS - URL"
                String record = sdf.format(new Date(visitTimeMillis)) + " - " + url;

                // 기존에 없는 새로운 기록만 저장
                if (!existingRecords.contains(record)) {
                    newRecords.add(record);
                }
            }

        } catch (SQLException e) {
            System.out.println("데이터베이스 오류: " + e.getMessage());
            throw e; // SQLException 발생 시 예외 던지기
        }

        // 4. 새로운 기록을 오름차순으로 정렬
        Collections.sort(newRecords);

        // 5. 정렬된 기록을 파일에 추가 (누적 저장)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true))) {
            for (String record : newRecords) {
                writer.write(record); // 기록을 파일에 작성
                writer.newLine(); // 줄바꿈 처리
            }
            System.out.println("새로운 방문기록이 저장되었습니다.");
        } catch (IOException e) {
            System.out.println("파일 쓰기 오류: " + e.getMessage());
        }
    }

    // 기존 방문기록을 로드하고 마지막 방문 시간을 반환하는 메서드
    private long loadExistingRecords(Set<String> existingRecords) {
        long lastVisitTime = 0; // 마지막 방문 시간 초기화 (기본값: 0)

        try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_FILE_PATH))) {
            String line; // 파일의 각 줄을 읽기 위한 변수
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 파일을 한 줄씩 읽어 중복 방지용 Set에 저장
            while ((line = reader.readLine()) != null) {
                existingRecords.add(line); // 기존 기록을 Set에 추가

                // 마지막 줄의 방문 시간을 파싱
                int delimiterIndex = line.indexOf(" - "); // " - " 구분자 위치
                if (delimiterIndex != -1) {
                    String dateString = line.substring(0, delimiterIndex); // 날짜 부분 추출
                    Date visitDate = sdf.parse(dateString); // 문자열을 Date 객체로 변환
                    lastVisitTime = visitDate.getTime(); // 밀리초로 변환하여 저장
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("기존 방문기록 파일이 없습니다. 새로 생성합니다.");
        } catch (IOException | java.text.ParseException e) {
            System.out.println("기존 기록 로드 오류: " + e.getMessage());
        }

        return lastVisitTime; // 마지막 방문 시간 반환
    }

    // 데이터베이스 파일을 복사하는 메서드
    private void copyDatabase(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile(); // 대상 파일이 없으면 새로 생성
        }

        // 파일 채널을 이용한 파일 복사
        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size()); // 복사 수행
        }
        System.out.println("데이터베이스가 성공적으로 복사되었습니다.");
    }
}
