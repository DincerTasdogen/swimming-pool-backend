package com.sp.SwimmingPool.exception;

public class InvalidMedicalReportException extends AuthenticationException {
    public InvalidMedicalReportException(String message) {
        super(message, "REG_008");
    }
}

