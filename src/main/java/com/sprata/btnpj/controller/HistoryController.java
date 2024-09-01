package com.sprata.btnpj.controller;

import com.sprata.btnpj.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/history")
@EnableAsync
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @GetMapping("/export")
    public String exportHistory() {
        try {
            historyService.extractHistoryToFile();
            return "History exported successfully to chrome_history.txt";
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return "Error exporting history: " + e.getMessage();
        }
    }

    @GetMapping("/youtube-urls")
    public String extractYouTubeUrls() {
        try {
            historyService.extractYouTubeUrls();
            return "YouTube URLs extracted and logged successfully.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error extracting YouTube URLs: " + e.getMessage();
        }
    }

    @GetMapping("/extract-youtube-details")
    public String extractYouTubeDetails() {
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    historyService.extractYouTubeUrls().join(); // .join()으로 비동기 작업 완료까지 대기
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            future.get(); // 비동기 작업이 완료될 때까지 기다림
            return "YouTube video details extraction initiated. Check youtube_details.txt for results.";

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Error initiating YouTube video details extraction: " + e.getMessage();
        }
    }
}
