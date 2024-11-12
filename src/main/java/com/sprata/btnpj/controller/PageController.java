package com.sprata.btnpj.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprata.btnpj.service.NounService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private NounService nounService;

    @GetMapping("/MainPage")
    public String showMainPage(Model model) {
        try {
            // NounService에서 이미 생성된 카테고리 구조를 리스트 형식으로 가져옵니다.
            List<JsonNode> categoriesList = nounService.getCategoriesList();


            // categoriesList를 모델에 추가
            model.addAttribute("categories", categoriesList);
        } catch (IOException e) {
            e.printStackTrace();  // 예외 처리
        }

        return "MainPage1"; // Thymeleaf 템플릿 이름
    }

    @GetMapping("/startPage")
    public String WalkPage() {


        return "WalkthroughPage.html";
    }

    @GetMapping("/BeforePage")
    public String BeforePage() {


        return "BeforeAnalysisPage.html";
    }

    @GetMapping("/LoadingPage")
    public String LoadingPage() {


        return "LoadingPage.html";
    }
}