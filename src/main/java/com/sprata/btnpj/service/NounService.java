// ex1_details.txt(유튜브 세부 데이터 2개 정도만 넣어놓은 예시 파일) 파일을 json 형식으로 바꿨음
// 웹에 실행시키면 실행결과 출력했음
package com.sprata.btnpj.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class NounService {

    // YouTube 데이터가 저장된 파일 경로
    private static final String DATA_FILE_PATH = "C:/SpringProject/personalCookie/btnPJ/ex1_details.txt";

    /**
     * 비디오 데이터를 JSON 형식의 List로 반환하는 메소드
     *
     * @return JSON 형식의 비디오 데이터 리스트
     */
    public List<JsonNode> getVideoDataAsJson() {
        List<JsonNode> videoDataList = new ArrayList<>(); // 결과를 저장할 리스트

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
            String line;
            ObjectMapper mapper = new ObjectMapper(); // JSON 변환에 사용
            ObjectNode videoNode = null; // 각 비디오 항목을 저장할 JSON 객체

            // 파일을 한 줄씩 읽어들임
            while ((line = reader.readLine()) != null) {

                // 빈 줄을 만나면 현재 비디오 항목을 리스트에 추가하고 초기화
                if (line.isEmpty()) {
                    if (videoNode != null) {
                        videoDataList.add(videoNode); // 비디오 항목을 리스트에 추가
                        videoNode = null; // 새로운 항목을 위해 초기화
                    }
                } else if (line.startsWith("Date:")) { // 날짜 라인을 찾음
                    videoNode = mapper.createObjectNode(); // 새로운 비디오 항목을 위한 JSON 객체 생성
                    videoNode.put("Date", line.substring(6).trim()); // "Date" 필드를 JSON 객체에 추가
                } else if (line.startsWith("URL:")) { // URL 라인을 찾음
                    videoNode.put("URL", line.substring(5).trim()); // "URL" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Title:")) { // 제목 라인을 찾음
                    videoNode.put("Title", line.substring(7).trim()); // "Title" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Thumbnail:")) { // 썸네일 라인을 찾음
                    videoNode.put("Thumbnail", line.substring(11).trim()); // "Thumbnail" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Categories:")) { // 카테고리 라인을 찾음
                    String categoriesString = line.substring(12).trim();
                    ArrayNode categoriesNode = mapper.createArrayNode(); // 카테고리를 배열 형태로 저장
                    categoriesString = categoriesString.substring(1, categoriesString.length() - 1); // 괄호 제거
                    for (String category : categoriesString.split(",")) {
                        categoriesNode.add(category.trim().replace("\"", "")); // 카테고리 문자열에서 큰따옴표 제거 후 추가
                    }
                    videoNode.set("Categories", categoriesNode); // "Categories" 배열을 JSON 객체에 추가
                } else if (line.startsWith("Tags:")) { // 태그 라인을 찾음
                    String tagsString = line.substring(6).trim();
                    ArrayNode tagsNode = mapper.createArrayNode(); // 태그를 배열 형태로 저장
                    tagsString = tagsString.substring(1, tagsString.length() - 1); // 괄호 제거
                    for (String tag : tagsString.split(",")) {
                        tagsNode.add(tag.trim().replace("\"", "")); // 태그 문자열에서 큰따옴표 제거 후 추가
                    }
                    videoNode.set("Tags", tagsNode); // "Tags" 배열을 JSON 객체에 추가
                }
            }

            // 파일의 마지막 비디오 항목이 리스트에 추가되지 않은 경우 추가
            if (videoNode != null) {
                videoDataList.add(videoNode); // 마지막 비디오 항목을 리스트에 추가
            }

        } catch (Exception e) {
            e.printStackTrace(); // 예외가 발생하면 콘솔에 오류 메시지 출력
        }

        return videoDataList; // JSON 형식의 비디오 데이터 리스트 반환
    }
}
