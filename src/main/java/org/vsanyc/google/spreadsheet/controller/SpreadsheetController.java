package org.vsanyc.google.spreadsheet.controller;

import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.vsanyc.google.spreadsheet.domain.SpreadsheetForm;
import org.vsanyc.google.spreadsheet.service.SpreadsheetService;


@RestController
@AllArgsConstructor
public class SpreadsheetController {

    private SpreadsheetService spreadsheetService;

    @PostMapping("/spreadsheet/copy")
    public AppendValuesResponse copySpreadsheet(@RequestBody SpreadsheetForm spreadsheetForm) throws Exception {
        return spreadsheetService.createFilteredCopy(spreadsheetForm);
    }

}

