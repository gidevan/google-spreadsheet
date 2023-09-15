package org.vsanyc.google.spreadsheet.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.vsanyc.google.spreadsheet.domain.SpreadsheetForm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class SpreadsheetService {

    private static final String APPLICATION_NAME = "ApplicationName";

    //private static final String SPREADSHEET_ID = "1hzWtZXPyU8uuYJ88C2LMmmb9clJ_nrm1p05HI0wiBIM";

    /**
     * Url
     */
    private static final String URL_DOC_ = "https://docs.google.com/spreadsheets/u/0/d/1hzWtZXPyU8uuYJ88C2LMmmb9clJ_nrm1p05HI0wiBIM/htmlview";

    private static final String TEST_DOC_URL = "https://docs.google.com/spreadsheets/d/1Xpn4jgytty9ufJ1TMqK02sS8MbfoiRKRcX54o88pP8g/edit#gid=0";

    //private static final String SPREADSHEET_ID = "1Xpn4jgytty9ufJ1TMqK02sS8MbfoiRKRcX54o88pP8g";

    private static final String RESULT_SHEET_URL = "https://docs.google.com/spreadsheets/d/12tSk8Q62BVWWvIUNHadWf-zefrVWU8EQ88gy0axdFP8/edit#gid=0";

    //private static final String RESULT_SHEET_ID = "12tSk8Q62BVWWvIUNHadWf-zefrVWU8EQ88gy0axdFP8";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final String CREDENTIALS_FILE_PATH = "google-sheets-client-secret.json";

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);

    private static NetHttpTransport HTTP_TRANSPORT = null;

    private static final int FAMILY_COLUMN_NUMBER = 2;
    private static final Map<String, List<Object>> parsedRows = new LinkedHashMap<>();


    private List<String> filterNames = Stream.of("Селянкин Владимир",
            "Штолин Юрий", "Зайцев Миша")
            .map(String :: toLowerCase).
            collect(Collectors.toList());


    public SpreadsheetService() {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            log.error("Error creating");
        }
    }

    public String createCopy(SpreadsheetForm spreadsheetForm) throws Exception {

        final List<String> ranges = Arrays.asList(spreadsheetForm.getRange());
        Sheets service =
                new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        var response = service.spreadsheets().values()
                .batchGet(spreadsheetForm.getSourceSpreadSheetId())
                .setRanges(ranges)
                .execute();
        List<ValueRange> values = response.getValueRanges();
        if (!CollectionUtils.isEmpty(values)) {
            copyToNewSheet(spreadsheetForm.getResultSpreadSheetId(), spreadsheetForm.getRange(), values);
        } else {
            throw new IllegalArgumentException("No data found for: " + spreadsheetForm.getSourceSpreadSheetId());
        }
        return "Values size: " + values.size();
    }

    private void copyToNewSheet(String resultSpreadsheetId, String range, List<ValueRange> values)  throws IOException {
        Sheets service =
                new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        for(ValueRange value: values) {
            var valueToSave = filterRows(range, value);

            var result = service.spreadsheets().values()
                    .append(resultSpreadsheetId, range, valueToSave)

                    .setValueInputOption("RAW")
                    .execute();
            log.info("Updated cells: {}", result.getTableRange());

        }
    }

    private ValueRange filterRows(String range, ValueRange valueRange) {
        var valueRangeToSave = new ValueRange();
        var rows = valueRange.getValues();

        boolean isFirstRow = true;
        for (var row : rows) {
            if (isFirstRow || filterRow(row)) {
                var familyColumnValue = row.get(FAMILY_COLUMN_NUMBER).toString().toLowerCase().trim();
                parsedRows.put(familyColumnValue, row);

            }
            isFirstRow = false;
        }
        var filteredRows = parsedRows.values().stream().toList();
        valueRangeToSave.setValues(filteredRows);
        valueRangeToSave.setMajorDimension(valueRange.getMajorDimension());
        valueRangeToSave.setRange(range);
        return valueRangeToSave;
    }

    private boolean filterRow(List<Object> row) {
        var familyColumnValue = row.get(FAMILY_COLUMN_NUMBER);
        if (familyColumnValue != null) {
            var family = familyColumnValue.toString().toLowerCase().trim();

            return filterNames.contains(family);
        } else {
            return false;
        }
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = SpreadsheetService.class.getClassLoader().getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")

                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

}
