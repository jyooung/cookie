package com.sprata.btnpj.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprata.btnpj.service.NounService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/cate") // API 엔드포인트의 기본 경로 설정
@RestController // 이 클래스가 RESTful 웹 서비스의 컨트롤러임을 나타냄
public class NounController {

    @Autowired // NounService를 자동 주입
    private NounService nounService;

    @GetMapping("/top-categories") // HTTP GET 요청을 처리하는 메소드
    public List<JsonNode> getTopCategories() {
        return nounService.getVideoDataAsJson(); // 비디오 데이터를 JSON 형식으로 반환
    }
}
