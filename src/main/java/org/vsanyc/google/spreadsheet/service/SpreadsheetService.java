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
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.vsanyc.google.spreadsheet.domain.SpreadsheetForm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpreadsheetService {

    private static final String APPLICATION_NAME = "ApplicationName";

    /**
     * Url
     */
    private static final String URL_DOC_ = "https://docs.google.com/spreadsheets/u/0/d/1hzWtZXPyU8uuYJ88C2LMmmb9clJ_nrm1p05HI0wiBIM/htmlview";

    private static final String TEST_DOC_URL = "https://docs.google.com/spreadsheets/d/1Xpn4jgytty9ufJ1TMqK02sS8MbfoiRKRcX54o88pP8g/edit#gid=0";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);

    private static NetHttpTransport HTTP_TRANSPORT = null;

    private static final int FAMILY_COLUMN_NUMBER = 2;
    private static final Map<String, List<Object>> parsedRows = new LinkedHashMap<>();

    @Value("${config.filter.pupils.path}")
    private String configPupilsPath;

    @Value("${config.google.credentials.path}")
    private String googleCredentialsPath;

    private List<String> filterNames;

    public SpreadsheetService() {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            log.error("Error creating google connection");
        }
    }

    @PostConstruct
    public void init() throws IOException, URISyntaxException{
        log.info("Post construct read pupils to filter");
        filterNames = readPupils();
    }

    public AppendValuesResponse createFilteredCopy(SpreadsheetForm spreadsheetForm) throws Exception {

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
            var updateDocResponse = copyToNewSheet(spreadsheetForm.getResultSpreadSheetId(), spreadsheetForm.getRange(), values);
            return updateDocResponse;
        } else {
            throw new IllegalArgumentException("No data found for: " + spreadsheetForm.getSourceSpreadSheetId());
        }

    }

    private AppendValuesResponse copyToNewSheet(String resultSpreadsheetId, String range, List<ValueRange> values)  throws IOException {
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
            return result;
        }
        return null;
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

    private List<String> readPupils() throws IOException, URISyntaxException {
        try {
            log.info("Try to open pupil config file [{}]", configPupilsPath);
            var rows = readFilterPupils();
            return rows.stream().map(row -> row.toLowerCase().trim()).collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            log.error("Error reading pupils from file [{}]", configPupilsPath);
            throw e;
        }
    }

    private List<String> readFilterPupils() throws IOException, URISyntaxException {
        String userDir = System.getProperty("user.dir");
        var configFile = new File(userDir + "/" + configPupilsPath);
        if (configFile.exists() && !configFile.isDirectory()) {
            return Files.readAllLines(configFile.toPath());
        } else {
            var url = getClass().getClassLoader().getResource(configPupilsPath);
            return Files.readAllLines(Path.of(url.toURI()));
        }
    }

    private InputStream readGoogleCredentials() throws FileNotFoundException {
        String userDir = System.getProperty("user.dir");
        var configFile = new File(userDir + "/" + googleCredentialsPath);
        if (configFile.exists() && !configFile.isDirectory()) {
            return new FileInputStream(userDir + "/" + googleCredentialsPath);
        } else {
            return SpreadsheetService.class.getClassLoader().getResourceAsStream(googleCredentialsPath);
        }
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = readGoogleCredentials();
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + googleCredentialsPath);
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
