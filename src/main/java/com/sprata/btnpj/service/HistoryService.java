// 원래 크롬 방문 기록 가져오는 건 다 해결했다고 생각했는데 중복저장으로 생각되는 기록들이 너무 많았음
// 현재는 방문시간과 url이 일치하는 데이터는 하나만 남기고 제거하는 작업했음

package com.sprata.btnpj.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

@Service
public class HistoryService {

    // 원본 크롬 방문기록 DB 경로 (Chrome의 History 파일 경로)
    private static final String ORIGINAL_DB_FILE_PATH =
            "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History";

    // 복사본 DB 파일 경로 (SQLite에서 접근할 파일)
    private static final String COPIED_DB_FILE_PATH =
            "C:/Users/LG/AppData/Local/Google/Chrome/User Data/Default/History_copy";

    // SQLite DB URL 생성 (복사된 DB 파일을 이용)
    private static final String DB_URL = "jdbc:sqlite:" + COPIED_DB_FILE_PATH;

    // 크롬 방문기록을 저장할 파일 경로 (누적 저장)
    private static final String OUTPUT_FILE_PATH = "chrome_history5.txt";

    /**
     * 크롬 방문기록을 추출하고 파일에 저장하는 메서드.
     * 중복된 기록은 제외하고, 마지막 방문 이후의 새로운 기록만 추가로 저장.
     */
    public void extractHistoryToFile() throws SQLException, IOException {
        // 1. 원본 DB를 복사하여 안전한 읽기 작업을 보장.
        copyDatabase(new File(ORIGINAL_DB_FILE_PATH), new File(COPIED_DB_FILE_PATH));

        // 복사 후 500ms 대기 (파일 시스템 지연 문제 방지)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 시 스레드 종료 처리
        }

        // 2. 기존 기록 불러오기 및 마지막 방문 시간 확인
        Set<String> existingRecords = new HashSet<>();  // 중복 방지를 위한 Set 사용
        long lastVisitTime = loadExistingRecords(existingRecords);  // 마지막 방문 시간(밀리초)을 로드

        // 마지막 방문 기록 출력 (디버깅용)
        if (lastVisitTime > 0) {
            String lastVisitDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastVisitTime));
            System.out.println("가장 마지막 방문 날짜: " + lastVisitDate);
        } else {
            System.out.println("기존 기록이 없습니다. 모든 기록을 가져옵니다.");
        }

        // 3. DB에서 마지막 방문 이후의 새로운 기록만 조회
        List<String> newRecords = new ArrayList<>();  // 새로운 방문기록 저장용 리스트

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            System.out.println("SQLite 연결 성공.");

            // SQL 쿼리: 마지막 방문 이후의 기록만 가져오고 오름차순 정렬
            String query = "SELECT urls.url, visits.visit_time " +
                    "FROM urls INNER JOIN visits ON urls.id = visits.url " +
                    "WHERE visits.visit_time / 1000 - 11644473600000 > " + lastVisitTime +
                    " ORDER BY visits.visit_time ASC";

            // 쿼리 실행 및 결과 처리
            ResultSet rs = stmt.executeQuery(query);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // ResultSet 순회하며 각 방문기록 처리
            while (rs.next()) {
                String url = rs.getString("url");  // 방문한 URL
                long visitTimeMicroseconds = rs.getLong("visit_time");  // 방문 시간(마이크로초 단위)
                long visitTimeMillis = visitTimeMicroseconds / 1000L - 11644473600000L;  // Windows epoch 보정

                // "YYYY-MM-DD HH:MM:SS - URL" 형식으로 기록 생성
                String record = sdf.format(new Date(visitTimeMillis)) + " - " + url;

                // Set에 없는 새로운 기록만 추가
                if (!existingRecords.contains(record)) {
                    newRecords.add(record);  // 새로운 기록을 리스트에 추가
                }
            }

        } catch (SQLException e) {
            System.out.println("데이터베이스 오류: " + e.getMessage());
            throw e;  // 예외 발생 시 SQLException 던지기
        }

        // 4. 새로운 기록을 오름차순으로 정렬
        Collections.sort(newRecords);

        // 5. 정렬된 기록을 파일에 추가 저장 (기존 기록에 누적)
        appendRecordsToFile(newRecords, OUTPUT_FILE_PATH);
    }

    /**
     * 기존 방문기록 파일에서 기록을 로드하고 마지막 방문 시간을 반환.
     * @param existingRecords 기존 기록을 저장할 Set
     * @return 마지막 방문 시간(밀리초)
     */
    // 기존 방문기록을 로드하고 마지막 방문 시간을 반환하는 메서드
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
                        existingRecords.add(recordKey); // 중복 방지 Set에도 추가
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
            // 파일이 존재하지 않을 경우 처리
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
        Collections.sort(uniqueRecords, (a, b) -> {
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
        } catch (IOException e) {
            // 파일 쓰기 오류 처리
            System.out.println("파일 쓰기 오류: " + e.getMessage());
        }
    }

    /**
     * 새로운 기록을 파일에 추가 저장하는 메서드 (누적 저장).
     * @param records 추가할 새로운 기록 리스트
     * @param filePath 파일 경로
     */
    private void appendRecordsToFile(List<String> records, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (String record : records) {
                writer.write(record);  // 기록 작성
                writer.newLine();  // 줄바꿈 추가
            }
            System.out.println(filePath + "에 새로운 기록이 저장되었습니다.");
        } catch (IOException e) {
            System.out.println("파일 쓰기 오류: " + e.getMessage());
        }
    }

    /**
     * 데이터베이스 파일을 복사하는 메서드.
     * @param sourceFile 원본 파일
     * @param destFile 대상 파일
     * @throws IOException 파일 복사 중 오류 발생 시
     */
    private void copyDatabase(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();  // 대상 파일이 없으면 새로 생성
        }

        // 파일 채널을 사용해 파일 복사
        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());  // 파일 복사 수행
        }
        System.out.println("데이터베이스가 성공적으로 복사되었습니다.");
    }
}
