package com.sprata.btnpj.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprata.btnpj.service.NounService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NounController 클래스는 비디오 데이터와 관련된 명사 및 카테고리를 처리하는 RESTful API입니다.
 */
@RequestMapping("/api/cate") // 기본 API 경로 설정
@RestController // 이 클래스가 REST 컨트롤러임을 나타냄
public class NounController {

    // NounService 객체 자동 주입
    private final NounService nounService;

    // NounService 의존성 주입
    @Autowired
    public NounController(NounService nounService) {
        this.nounService = nounService;
    }

    /**
     * 비디오 데이터에서 추출된 명사와 관련된 모든 비디오 데이터를 반환하는 메소드.
     *
     * @return 명사가 추출된 비디오 데이터 리스트
     */
    @GetMapping("/video-data-noun") // GET 요청에 대한 처리 메소드
    public ResponseEntity<List<JsonNode>> getVideoDataWithExtractedNouns() {
        // NounService의 getVideoDataWithExtractedNouns 메소드를 호출하여
        // 명사 추출된 비디오 데이터를 JSON 형식으로 반환
        List<JsonNode> videoData = nounService.getVideoDataWithExtractedNouns();
        return new ResponseEntity<>(videoData, HttpStatus.OK);
    }

    /**
     * 각 비디오 데이터에서 상위 10개 하위 카테고리를 반환하는 메소드.
     *
     * @return 상위 10개 하위 카테고리 리스트
     */
    @GetMapping("/top-sub-categories") // 하위 카테고리 조회 엔드포인트
    public ResponseEntity<List<String>> getTopSubCategories() {
        // NounService의 getTopSubCategories 메소드를 호출하여 상위 10개 하위 카테고리를 반환
        List<String> topSubCategories = nounService.getTopSubCategories();
        return new ResponseEntity<>(topSubCategories, HttpStatus.OK);
    }

    /**
     * 각 비디오 데이터에서 상위 카테고리를 반환하는 메소드.
     *
     * @return 상위 카테고리 리스트
     */
    @GetMapping("/top-main-categories") // 상위 카테고리 조회 엔드포인트
    public ResponseEntity<List<String>> getTopMainCategories() {
        // NounService의 getTopMainCategories 메소드를 호출하여 상위 카테고리를 반환
        List<String> topMainCategories = nounService.getTopMainCategories();
        return new ResponseEntity<>(topMainCategories, HttpStatus.OK);
    }

    /**
     * 비디오 데이터를 상위 및 하위 카테고리별로 그룹화하고 구조를 JSON 형식으로 출력하는 메서드.
     *
     * @return 카테고리 구조 생성 완료 메시지
     */
    @GetMapping("/generate-category-structure") // 카테고리 구조 생성을 위한 엔드포인트
    public ResponseEntity<String> generateCategoryStructure() {
        // 비디오 데이터 리스트를 가져옴
        List<JsonNode> videoDataList = nounService.getVideoDataWithExtractedNouns();

        // NounService의 generateCategoryStructure 메서드에 비디오 데이터 전달
        nounService.generateCategoryStructure(videoDataList);

        // 카테고리 구조 생성 및 출력 완료 메시지 반환
        return new ResponseEntity<>("Category structure generated and printed successfully.", HttpStatus.OK);
    }

}
