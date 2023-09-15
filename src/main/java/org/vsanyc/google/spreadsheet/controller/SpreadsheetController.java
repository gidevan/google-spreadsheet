package org.vsanyc.google.spreadsheet.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vsanyc.google.spreadsheet.domain.SpreadsheetForm;
import org.vsanyc.google.spreadsheet.service.SpreadsheetService;


@RestController
@AllArgsConstructor
public class SpreadsheetController {

    private SpreadsheetService spreadsheetService;

    @GetMapping("/hello")
    public String hello() {
        return "hello!!!";
    }


    @PostMapping("/spreadsheet/copy")
    public String copySpreadsheet(SpreadsheetForm spreadsheetForm) throws Exception {
        return spreadsheetService.createCopy(spreadsheetForm);
    }

}

