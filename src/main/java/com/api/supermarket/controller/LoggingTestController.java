package com.api.supermarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/logging")
public class LoggingTestController {

    private static final Logger log = LoggerFactory.getLogger(LoggingTestController.class);

    @GetMapping
    public String demo() {
        log.info("Logging demo API was called");
        return "OK";
    }

    @GetMapping("/exception-message")
    public String testExceptionMessage() {
        try {
            triggerDivideByZero();
        } catch (Exception e) {
            log.error("Lỗi chia cho 0: {}", e.getMessage());
        }
        return "Đã ghi log exception thành công. Vui lòng kiểm tra file log.";
    }

    @GetMapping("/exception-stacktrace")
    public String testExceptionStackTrace() {
        try {
            triggerDivideByZero();
        } catch (Exception e) {
            log.error("Exception occurred with stack trace", e);
        }
        return "Đã ghi log exception stack trace thành công. Vui lòng kiểm tra file log.";
    }

    private void triggerDivideByZero() {
        int a = 10;
        int b = 0;
        if (b == 0) {
            throw new ArithmeticException("/ by zero");
        }
        log.info("Result: {}", a / b);
    }
}
