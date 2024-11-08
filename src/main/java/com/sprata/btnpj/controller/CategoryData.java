package com.sprata.btnpj.controller;

import java.util.List;

public class CategoryData {
    private String mainCategory;
    private List<SubCategory> subCategories;

    // getters and setters

    public static class SubCategory {
        private String subCategory;
        private List<Video> videos;

        // getters and setters
    }

    public static class Video {
        private String date;
        private String url;
        private String title;
        private String thumbnail;
        private List<String> tags;
        private List<String> extractedNouns;

        // getters and setters
    }
}
