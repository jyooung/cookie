// 워크스루, 분석 전, 로딩, 메인페이지 연결 완료
// new 페이지 연결하기 그리고 많은 데이터 체험해보기
package com.sprata.btnpj.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprata.btnpj.service.NounService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class NaviController {

    @GetMapping("/walkthroughPage1")
    public String showWalkthroughPage() {
        return "WalkthroughPage";  // WalkthroughPage.html로 이동
    }

    @GetMapping("/beforeAnalysisPage")
    public String showBeforeAnalysisPage() {
        return "BeforeAnalysisPage";  // BeforeAnalysisPage.html로 이동
    }

    @GetMapping("/loadingPage")
    public String showLoadingPage() {
        // 데이터 처리 시작
        nounService.processData();
        return "LoadingPage";  // loadingPage.html로 이동
    }

    @GetMapping("/startAnalysis")
    @ResponseBody
    public ResponseEntity<String> checkAnalysisStatus() {
        // 데이터 준비 상태를 확인
        if (nounService.isDataReady()) {
            return ResponseEntity.ok("Data is ready");
        } else {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Data is still processing");
        }
    }

    @Autowired
    private NounService nounService;

    @GetMapping("/mainPage")
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
}

