package in.sb.pinac.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Configuration
public class GoogleConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleConfig.class);
    private static final String APPLICATION_NAME = "PINAC LMS Google Sheets Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE
    );

    @Value("${google.credentials.path:classpath:google/credentials.json}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public GoogleConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Initializes GoogleCredentials from credentials.json securely.
     */
    @Bean
    public GoogleCredentials googleCredentials() {
        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                log.warn("Google Credentials: {} not found. Google Sheets API will run in fallback mode.", credentialsPath);
                return null;
            }
            try (InputStream in = resource.getInputStream()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(SCOPES);
                log.info("Google Cloud Service Account authenticated successfully from {}", credentialsPath);
                return credentials;
            }
        } catch (Exception e) {
            log.error("Google Credentials initialization error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Google Sheets API Service Bean
     */
    @Bean
    public Sheets sheetsService(GoogleCredentials credentials) {
        if (credentials == null) {
            return null;
        }
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            log.info("Google Sheets API service initialized successfully.");
            return service;
        } catch (Exception e) {
            log.error("Google Sheets Service initialization error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Google Drive API Service Bean
     */
    @Bean
    public Drive driveService(GoogleCredentials credentials) {
        if (credentials == null) {
            return null;
        }
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            Drive service = new Drive.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            log.info("Google Drive API service initialized successfully.");
            return service;
        } catch (Exception e) {
            log.error("Google Drive Service initialization error: {}", e.getMessage(), e);
            return null;
        }
    }
}
