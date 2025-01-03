package com.sprata.btnpj.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprata.btnpj.service.NounService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class PageController {

    @Autowired
    private NounService nounService;

    @GetMapping("/MainPage")
    public String showMainPage(Model model) {

        try {
            // NounService에서 기존 카테고리 리스트를 가져옵니다.
            List<JsonNode> categoriesList = nounService.getCategoriesList();
            model.addAttribute("categories", categoriesList);

            // cateOutput.json 파일을 읽어옵니다.
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream cateOutputStream = getClass().getResourceAsStream("/cateOutput.json");
            Map<String, Object> cateOutputData;

            if (cateOutputStream != null) {
                cateOutputData = objectMapper.readValue(cateOutputStream, new TypeReference<>() {});
            } else {
                cateOutputData = new HashMap<>(); // 파일이 없으면 기본값 사용
                System.err.println("cateOutput.json 파일이 존재하지 않습니다. 빈 데이터로 초기화합니다.");
            }

            model.addAttribute("cateOutput", cateOutputData);

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "데이터를 불러오는 중 오류가 발생했습니다.");
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