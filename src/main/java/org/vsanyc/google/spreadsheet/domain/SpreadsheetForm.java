package org.vsanyc.google.spreadsheet.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpreadsheetForm {
    private String sourceSpreadSheetId;
    private String resultSpreadSheetId;
    private String range;
}
