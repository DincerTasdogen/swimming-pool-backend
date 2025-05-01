package com.sp.SwimmingPool.model.enums;

public enum RiskLevel {
    LOW("Düşük Risk"),
    MEDIUM("Ortalama Risk"),
    HIGH("Yüksek Risk");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

