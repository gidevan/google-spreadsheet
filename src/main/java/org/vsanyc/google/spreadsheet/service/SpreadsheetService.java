package org.vsanyc.google.spreadsheet.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SpreadsheetService {

    private static final String APPLICATION_NAME = "ApplicationName";

    private static final String SPREADSHEET_ID = "1hzWtZXPyU8uuYJ88C2LMmmb9clJ_nrm1p05HI0wiBIM";

    private static final String TEST_DOC_URL = "https://docs.google.com/spreadsheets/d/1Xpn4jgytty9ufJ1TMqK02sS8MbfoiRKRcX54o88pP8g/edit#gid=0";

    //private static final String SPREADSHEET_ID = "1Xpn4jgytty9ufJ1TMqK02sS8MbfoiRKRcX54o88pP8g";

    private static final String RESULT_SHEET_URL = "https://docs.google.com/spreadsheets/d/12tSk8Q62BVWWvIUNHadWf-zefrVWU8EQ88gy0axdFP8/edit#gid=0";

    private static final String RESULT_SHEET_ID = "12tSk8Q62BVWWvIUNHadWf-zefrVWU8EQ88gy0axdFP8";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final String CREDENTIALS_FILE_PATH = "google-sheets-client-secret.json";

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final String RANGE = "A1:B3";

    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);

    private static NetHttpTransport HTTP_TRANSPORT = null;

    public SpreadsheetService() {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            log.error("Error creting");
        }

    }

    public static UpdateValuesResponse updateValues(String spreadsheetId,
                                                    String range,
                                                    ValueRange valueRange)
            throws IOException {
        /* Load pre-authorized user credentials from the environment.
           TODO(developer) - See https://developers.google.com/identity for
            guides on implementing OAuth2 for your application. */
        InputStream in = SpreadsheetService.class.getClassLoader().getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(
                credentials);

        // Create the sheets API client
        Sheets service = new Sheets.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Sheets samples")
                .build();

        UpdateValuesResponse result = null;
        try {
            // Updates the values in the specified range.
            ValueRange body = new ValueRange()
                    .setValues(valueRange.getValues());
            result = service.spreadsheets().values().update(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();
            System.out.printf("%d cells updated.", result.getUpdatedCells());
        } catch (GoogleJsonResponseException e) {
            // TODO(developer) - handle error appropriately
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 404) {
                System.out.printf("Spreadsheet not found with id '%s'.\n", spreadsheetId);
            } else {
                throw e;
            }
        }
        return result;
    }

    public String createCopy(String url) throws Exception {

        final List<String> ranges = Arrays.asList(RANGE);
        Sheets service =
                new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        var response = service.spreadsheets().values()
                .batchGet(SPREADSHEET_ID)
                .setRanges(ranges)
                .execute();
        List<ValueRange> values = response.getValueRanges();
        if (!CollectionUtils.isEmpty(values)) {
            copyToNewSheet(values);
        } else {
            throw new IllegalArgumentException("No data found for: " + SPREADSHEET_ID);
        }
        return "Values size: " + String.valueOf(values.size());
    }

    private void copyToNewSheet(List<ValueRange> values)  throws IOException {
        Sheets service =
                new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        for(ValueRange value: values) {
            ValueRange valueToSave = new ValueRange();
            valueToSave.setValues(value.getValues());
            valueToSave.setRange(RANGE);
            valueToSave.setMajorDimension(value.getMajorDimension());

            var result = service.spreadsheets().values()
                    .append(RESULT_SHEET_ID, RANGE, valueToSave)

                    .setValueInputOption("RAW")
                    .execute();
            log.info("Updated cells: {}", result.getTableRange());

            //updateValues(RESULT_SHEET_ID, value.getRange(), value);
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
