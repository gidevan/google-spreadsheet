package org.vsanyc.google.spreadsheet;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class GoogleSpreadsheetApp {
    public static void main(String[] args) {
        SpringApplication.run(GoogleSpreadsheetApp.class, args);
    }
}