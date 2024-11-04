// ex1_details.txt(유튜브 세부 데이터 2개 정도만 넣어놓은 예시 파일) 파일을 json 형식으로 바꿨음
// 웹에 실행시키면 실행결과 출력했음
// 상위카테고리 리스트 생성하고 하위 카테고리 상위 10개 리스트 생성하기 완료
// 바로 이전 커밋이 하위 카테고리 리스트 생성까지임
// 이제 상위 카테고리와 하위 카테고리에 모두 적합한 영상 분류하면 끝
// new !! 카테고리 분류할 구조체 생성 완료
package com.sprata.btnpj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NounService {

    // YouTube 데이터가 저장된 파일 경로
    private static final String DATA_FILE_PATH = "C:/SpringProject/personalCookie/btnPJ/ex1_details.txt";

    // Python 스크립트에서 입력 데이터로 사용할 JSON 파일 경로
    private static final String INPUT_JSON_PATH = "C:/SpringProject/personalCookie/btnPJ/input.json";

    // Python 스크립트의 출력 결과를 받을 JSON 파일 경로
    private static final String OUTPUT_JSON_PATH = "C:/SpringProject/personalCookie/btnPJ/output.json";

    /**
     * ex1_details.txt 파일의 데이터를 JSON 형식으로 변환하여 input.json 파일로 저장하고
     * Python 스크립트를 실행하여 output.json에 저장된 결과를 읽어 반환
     *
     * @return Python 스크립트를 통해 명사가 추출된 비디오 데이터 리스트
     */
    public List<JsonNode> getVideoDataWithExtractedNouns() {
        // ex1_details.txt 파일을 읽어 JSON 형식으로 변환된 데이터 리스트를 가져옴
        List<JsonNode> videoDataList = parseVideoDataFromFile();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // 비디오 데이터 리스트를 input.json 파일로 저장하여 Python 스크립트에 전달
            mapper.writeValue(new FileWriter(INPUT_JSON_PATH), videoDataList);

            // Python 스크립트 실행하여 명사 추출 작업 수행
            String pythonScriptPath = "C:/SpringProject/personalCookie/btnPJ/src/main/resources/python/noun_extractor.py"; // 스크립트 경로 지정
            Process process = Runtime.getRuntime().exec("python " + pythonScriptPath);
            process.waitFor(); // Python 스크립트 실행 완료될 때까지 대기

            // Python 스크립트의 결과를 output.json 파일에서 읽음
            List<JsonNode> processedData;
            try (BufferedReader reader = new BufferedReader(new FileReader(OUTPUT_JSON_PATH ))) {
                // output.json 파일을 List<JsonNode> 형식으로 읽어들이기
                processedData = mapper.readValue(reader, mapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
            }

            // 결과를 반환
            return processedData;

        } catch (Exception e) {
            e.printStackTrace(); // 예외 발생 시 스택 트레이스 출력
        }

        // 오류가 발생했을 경우 원본 비디오 데이터를 반환
        return videoDataList;
    }

    /**
     * ex1_details.txt 파일을 읽어 JSON 형식의 List로 변환하는 메서드
     *
     * @return JSON 형식의 비디오 데이터 리스트
     */
    private List<JsonNode> parseVideoDataFromFile() {
        List<JsonNode> videoDataList = new ArrayList<>(); // 비디오 데이터 리스트 초기화

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
            String line; // 파일의 각 줄을 저장할 문자열 변수
            ObjectMapper mapper = new ObjectMapper(); // JSON 객체를 생성하기 위한 ObjectMapper
            ObjectNode videoNode = null; // 각 비디오 항목을 위한 JSON 객체

            // 파일을 한 줄씩 읽음
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    // 빈 줄을 만나면 현재 비디오 항목을 리스트에 추가하고 초기화
                    if (videoNode != null) {
                        videoDataList.add(videoNode);
                        videoNode = null; // 새로운 항목을 위해 초기화
                    }
                } else if (line.startsWith("Date:")) {
                    // 새로운 비디오 항목을 위한 JSON 객체 생성
                    videoNode = mapper.createObjectNode();
                    videoNode.put("Date", line.substring(6).trim()); // "Date" 필드를 JSON 객체에 추가
                } else if (line.startsWith("URL:")) {
                    // URL 필드를 JSON 객체에 추가
                    videoNode.put("URL", line.substring(5).trim()); // "URL" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Title:")) {
                    // Title 필드를 JSON 객체에 추가
                    videoNode.put("Title", line.substring(7).trim()); // "Title" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Thumbnail:")) {
                    // Thumbnail 필드를 JSON 객체에 추가
                    videoNode.put("Thumbnail", line.substring(11).trim()); // "Thumbnail" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Categories:")) {
                    // Categories 필드를 JSON 배열로 추가
                    String categoriesString = line.substring(12).trim();
                    ArrayNode categoriesNode = mapper.createArrayNode(); // 카테고리 배열 생성
                    categoriesString = categoriesString.substring(1, categoriesString.length() - 1); // 괄호 제거
                    for (String category : categoriesString.split(",")) {
                        categoriesNode.add(category.trim().replace("\"", "")); // 문자열에서 큰따옴표 제거 후 추가
                    }
                    videoNode.set("Categories", categoriesNode); // "Categories" 필드를 JSON 객체에 추가
                } else if (line.startsWith("Tags:")) {
                    // Tags 필드를 JSON 배열로 추가
                    String tagsString = line.substring(6).trim();
                    ArrayNode tagsNode = mapper.createArrayNode(); // 태그 배열 생성
                    tagsString = tagsString.substring(1, tagsString.length() - 1); // 괄호 제거
                    for (String tag : tagsString.split(",")) {
                        tagsNode.add(tag.trim().replace("\"", "")); // 문자열에서 큰따옴표 제거 후 추가
                    }
                    videoNode.set("Tags", tagsNode); // "Tags" 필드를 JSON 객체에 추가
                }
            }

            // 마지막 비디오 항목이 추가되지 않은 경우 추가
            if (videoNode != null) {
                videoDataList.add(videoNode); // 마지막 비디오 항목을 리스트에 추가
            }

        } catch (Exception e) {
            e.printStackTrace(); // 예외 발생 시 스택 트레이스 출력
        }

        return videoDataList; // JSON 형식의 비디오 데이터 리스트 반환
    }

    /**
     * output.json에서 각 URL에 해당하는 ExtractedNouns 필드를 모두 더하여 하위 카테고리 리스트를 생성하고
     * 각 카테고리의 빈도수를 계산하여 상위 10개 카테고리를 반환하는 메서드
     *
     * @return 상위 10개 카테고리 리스트
     */
    public List<String> getTopSubCategories() {
        List<JsonNode> extractedData = getVideoDataWithExtractedNouns(); // 추출된 데이터 가져오기
        Map<String, Integer> categoryFrequencyMap = new HashMap<>(); // 카테고리 빈도수 저장을 위한 HashMap

        // 각 비디오 데이터에서 ExtractedNouns를 기반으로 카테고리 빈도수 계산
        for (JsonNode videoData : extractedData) {
            JsonNode extractedNouns = videoData.get("ExtractedNouns"); // ExtractedNouns 필드 가져오기
            if (extractedNouns != null && extractedNouns.isArray()) {
                for (JsonNode noun : extractedNouns) {
                    String category = noun.asText().trim(); // 카테고리 이름 가져오기
                    if (!category.isEmpty()) { // 빈 문자열이 아닌 경우에만 추가
                        // 카테고리 빈도수 증가
                        categoryFrequencyMap.put(category, categoryFrequencyMap.getOrDefault(category, 0) + 1);
                    }
                }
            }
        }

        // 해시맵의 내용을 출력 (디버깅 용도)
        System.out.println("Category Frequency Map: " + categoryFrequencyMap);

        // 카테고리 빈도수를 기준으로 정렬하여 상위 10개 카테고리 추출
        return categoryFrequencyMap.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 빈도수 내림차순 정렬
                .limit(10) // 상위 10개만 선택
                .map(Map.Entry::getKey) // 카테고리 이름만 가져오기
                .toList(); // List<String>으로 변환하여 반환
    }

    /**
     * input.json의 각 데이터의 Categories 필드에서 상위 카테고리 리스트를 생성하고,
     * 각 상위 카테고리의 빈도수를 계산하여 내림차순으로 정렬된 상위 카테고리 리스트를 반환하는 메서드
     *
     * @return 내림차순으로 정렬된 상위 카테고리 리스트
     */
    public List<String> getTopMainCategories() {
        List<JsonNode> extractedData = getVideoDataWithExtractedNouns(); // 추출된 데이터 가져오기
        Map<String, Integer> mainCategoryFrequencyMap = new HashMap<>(); // 상위 카테고리 빈도수 저장을 위한 HashMap

        // 각 비디오 데이터에서 Categories 필드 기반으로 상위 카테고리 빈도수 계산
        for (JsonNode videoData : extractedData) {
            JsonNode categories = videoData.get("Categories"); // Categories 필드 가져오기
            if (categories != null && categories.isArray()) {
                for (JsonNode category : categories) {
                    String mainCategory = category.asText(); // 상위 카테고리 이름 가져오기
                    // 상위 카테고리 빈도수 증가
                    mainCategoryFrequencyMap.put(mainCategory, mainCategoryFrequencyMap.getOrDefault(mainCategory, 0) + 1);
                }
            }
        }

        // 해시맵의 내용을 출력 (디버깅 용도)
        System.out.println("Main Category Frequency Map: " + mainCategoryFrequencyMap);

        // 카테고리 빈도수를 기준으로 정렬하여 상위 카테고리 추출
        return mainCategoryFrequencyMap.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 빈도수 내림차순 정렬
                .map(Map.Entry::getKey) // 상위 카테고리 이름만 가져오기
                .toList(); // List<String>으로 변환하여 반환
    }
    /**
     * 상위 카테고리와 하위 카테고리 구조를 생성하고, 각 카테고리에 포함된 영상 객체를 정리하여 콘솔에 출력하는 메서드
     */
    public void generateCategoryStructure() {
        // 상위 카테고리와 하위 카테고리 리스트 가져오기
        List<String> topMainCategories = getTopMainCategories(); // 상위 카테고리 리스트
        List<String> topSubCategories = getTopSubCategories(); // 하위 카테고리 리스트

        // 카테고리 구조를 저장할 JSON 객체 생성
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode categoriesArrayNode = mapper.createArrayNode(); // 상위 카테고리를 위한 배열 노드

        // 각 상위 카테고리에 대해 하위 카테고리와 영상 객체를 포함한 구조 생성
        for (String mainCategory : topMainCategories) {
            ObjectNode mainCategoryNode = mapper.createObjectNode(); // 상위 카테고리 노드 생성
            mainCategoryNode.put("mainCategory", mainCategory); // 상위 카테고리 이름 설정

            ArrayNode subCategoriesArrayNode = mapper.createArrayNode(); // 하위 카테고리를 위한 배열 노드

            // 하위 카테고리를 최대 10개까지 추가
            for (int i = 0; i < Math.min(topSubCategories.size(), 10); i++) {
                String subCategory = topSubCategories.get(i); // 하위 카테고리 가져오기
                ObjectNode subCategoryNode = mapper.createObjectNode(); // 하위 카테고리 노드 생성
                subCategoryNode.put("subCategory", subCategory); // 하위 카테고리 이름 설정

                // 해당 하위 카테고리에 속하는 영상 객체 리스트 생성
                ArrayNode videoObjectsArrayNode = mapper.createArrayNode(); // 영상 객체 배열 노드 생성
                List<JsonNode> extractedData = getVideoDataWithExtractedNouns(); // 추출된 영상 데이터 가져오기

                // 각 영상 객체를 하위 카테고리에 추가
                for (JsonNode videoData : extractedData) {
                    JsonNode categories = videoData.get("Categories"); // 각 영상의 카테고리 가져오기
                    if (categories != null && categories.isArray()) {
                        // 영상이 현재 하위 카테고리에 속하는 경우
                        for (JsonNode category : categories) {
                            if (category.asText().equals(subCategory)) { // 하위 카테고리와 일치하는지 확인
                                ObjectNode videoObjectNode = mapper.createObjectNode(); // 영상 객체 노드 생성
                                // 영상 객체의 정보를 JSON 노드에 추가
                                videoObjectNode.put("Date", videoData.get("Date").asText());
                                videoObjectNode.put("URL", videoData.get("URL").asText());
                                videoObjectNode.put("Title", videoData.get("Title").asText());
                                videoObjectNode.put("Thumbnail", videoData.get("Thumbnail").asText());
                                videoObjectNode.set("Tags", videoData.get("Tags")); // 태그 필드 추가
                                videoObjectNode.set("ExtractedNouns", videoData.get("ExtractedNouns")); // 추출된 명사 필드 추가

                                videoObjectsArrayNode.add(videoObjectNode); // 영상 객체를 하위 카테고리 배열에 추가
                                break; // 현재 하위 카테고리에 속하는 영상을 찾았으므로 반복 종료
                            }
                        }
                    }
                }

                subCategoryNode.set("videos", videoObjectsArrayNode); // 하위 카테고리 노드에 영상 객체 배열 추가
                subCategoriesArrayNode.add(subCategoryNode); // 하위 카테고리 배열에 추가
            }

            mainCategoryNode.set("subCategories", subCategoriesArrayNode); // 상위 카테고리에 하위 카테고리 배열 추가
            categoriesArrayNode.add(mainCategoryNode); // 카테고리 배열에 상위 카테고리 추가
        }

        // 최종 카테고리 구조를 콘솔에 출력
        System.out.println("Generated Category Structure: " + categoriesArrayNode.toPrettyString());
    }

}
