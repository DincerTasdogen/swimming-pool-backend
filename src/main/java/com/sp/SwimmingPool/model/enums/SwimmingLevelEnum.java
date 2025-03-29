package com.sp.SwimmingPool.model.enums;

import lombok.Getter;

@Getter
public enum SwimmingLevelEnum {
    NONE("Yüzme Bilmiyor"),
    BEGINNER("Başlangıç"),
    INTERMEDIATE("Orta"),
    ADVANCED("İleri");

    private final String displayName;

    SwimmingLevelEnum(String displayName) {
        this.displayName = displayName;
    }

}