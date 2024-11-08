package com.sprata.btnpj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainPageController {

    @GetMapping("/")
    public String showMainPage() {
        return "MainPage";  // templates/MainPage.html 파일을 렌더링
    }

    @GetMapping("/MainPage")
    public String showMainPage1(Model model) {
        // 예시 상위 카테고리 리스트
        List<String> categories = Arrays.asList("여행", "요리", "운동", "예능", "패션");

        // 각 카테고리에 대한 하위 카테고리 데이터 예시
        Map<String, List<String>> subCategories = new HashMap<>();
        subCategories.put("여행", Arrays.asList("하위 1-1", "하위 1-2", "하위 1-3", "하위 1-4", "하위 1-5", "하위 1-6"));
        subCategories.put("요리", Arrays.asList("하위 2-1", "하위 2-2", "하위 2-3", "하위 2-4", "하위 2-5", "하위 2-6"));
        subCategories.put("운동", Arrays.asList("하위 3-1", "하위 3-2", "하위 3-3", "하위 3-4", "하위 3-5", "하위 3-6"));
        subCategories.put("예능", Arrays.asList("하위 4-1", "하위 4-2", "하위 4-3", "하위 4-4", "하위 4-5", "하위 4-6"));
        subCategories.put("패션", Arrays.asList("하위 5-1", "하위 5-2", "하위 5-3", "하위 5-4", "하위 5-5", "하위 5-6"));

        // 각 하위 카테고리에 해당하는 이미지 목록
        Map<String, List<String>> subCategoryImages = new HashMap<>();
        subCategoryImages.put("하위 1-1", Arrays.asList("이미지 1", "이미지 2"));
        subCategoryImages.put("하위 1-2", Arrays.asList("이미지 3", "이미지 4"));
        subCategoryImages.put("하위 2-1", Arrays.asList("이미지 A", "이미지 B"));
        subCategoryImages.put("하위 2-2", Arrays.asList("이미지 C", "이미지 D"));
        subCategoryImages.put("하위 3-1", Arrays.asList("이미지 X", "이미지 Y"));
        subCategoryImages.put("하위 3-2", Arrays.asList("이미지 Z", "이미지 W"));

        // 모델에 데이터 추가
        model.addAttribute("categories", categories);
        model.addAttribute("subCategories", subCategories);
        model.addAttribute("subCategoryImages", subCategoryImages);

        return "MainPage1"; // Thymeleaf 템플릿 이름
    }

}
