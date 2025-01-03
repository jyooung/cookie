// 워크스루, 분석 전, 로딩, 메인페이지 연결 완료
// new 페이지 연결하기 그리고 많은 데이터 체험해보기
package com.sprata.btnpj.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprata.btnpj.service.NounService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;

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
            // NounService에서 이미 생성된 카테고리 구조를 리스트 형식으로 가져옵니다.
            List<JsonNode> categoriesList = nounService.getCategoriesList();

            // categoriesList를 모델에 추가
            model.addAttribute("categories", categoriesList);
        } catch (IOException e) {
            e.printStackTrace();  // 예외 처리
        }

        return "MainPage1"; // MainPage1.html로 이동
    }
}

