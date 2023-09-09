package org.vsanyc.google.spreadsheet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vsanyc.google.spreadsheet.service.SpreadsheetService;

@RestController
public class SpreadsheetController {

    private SpreadsheetService spreadsheetService;
    public SpreadsheetController(SpreadsheetService spreadsheetService) {
        this.spreadsheetService = spreadsheetService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello!!!";
    }

    @GetMapping("/spreadsheet")
    public String spreadSheet() throws Exception{
        return spreadsheetService.createCopy("test.url");
    }

}

