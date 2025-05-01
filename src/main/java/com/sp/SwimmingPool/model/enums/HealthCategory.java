package com.sp.SwimmingPool.model.enums;

public enum HealthCategory {
    CARDIAC("Kalp Sağlığı"),
    NEUROLOGICAL("Nörolojik Sağlık"),
    MUSCULAR("Kas Sağlığı"),
    GENERAL("Genel Sağlık");

    private final String description;

    HealthCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
