package com.sprata.btnpj.controller;

import com.sprata.btnpj.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    @Autowired
    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * 브라우저 히스토리를 추출하고 날짜순으로 정렬하는 엔드포인트.
     * @return 성공 또는 실패 메시지
     */
    @GetMapping("/extract")
    public ResponseEntity<String> extractHistory() {
        try {
            historyService.extractHistoryToFile();
            return ResponseEntity.ok("Browser history extracted and sorted successfully.");
        } catch (SQLException | IOException e) {
            return ResponseEntity.status(500).body("Failed to extract browser history: " + e.getMessage());
        }
    }

    /**
     * YouTube URL 히스토리에서 상세 정보를 추출하고 날짜순으로 정렬하는 엔드포인트.
     * @return 작업 시작 메시지
     */
    @GetMapping("/extract-youtube-details")
    public ResponseEntity<String> extractYouTubeDetails() {
        try {
            CompletableFuture<Void> future = historyService.extractYouTubeUrls();
            return ResponseEntity.ok("YouTube details extraction started successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to extract YouTube details: " + e.getMessage());
        }
    }

    /**
     * 전체 브라우저 히스토리와 YouTube 상세 정보를 추출하고 모두 날짜순으로 정렬하는 엔드포인트.
     * @return 작업 시작 메시지
     */
    @GetMapping("/extract-all")
    public ResponseEntity<String> extractAll() {
        try {
            historyService.extractHistoryToFile();
            CompletableFuture<Void> future = historyService.extractYouTubeUrls();
            return ResponseEntity.ok("All history and YouTube details extraction started successfully.");
        } catch (SQLException | IOException e) {
            return ResponseEntity.status(500).body("Failed to extract all history: " + e.getMessage());
        }
    }
}
