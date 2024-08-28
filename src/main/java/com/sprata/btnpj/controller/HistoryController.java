package com.sprata.btnpj.controller;

import com.sprata.btnpj.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/history")
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
}
