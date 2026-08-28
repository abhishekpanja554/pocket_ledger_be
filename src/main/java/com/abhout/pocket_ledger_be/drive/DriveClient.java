package com.abhout.pocket_ledger_be.drive;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DriveClient {
    private static final String SCOPE =
            "https://www.googleapis.com/auth/drive.readonly";
    private static final String APPLICATION_NAME = "pocket-ledger";
    private static final String FIELDS = "files(id,name,mimeType,modifiedTime)";

    private final Drive drive;

    public DriveClient() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials
                .getApplicationDefault()
                .createScoped(List.of(SCOPE));
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        this.drive = new Drive.Builder(
                transport,
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName(APPLICATION_NAME).build();
    }

    public  List<DriveFile> listFiles(String folderId, Instant modifiedAfter) throws IOException{
        String query = "'%s' in parents and trashed = false".formatted(folderId);
        if ( modifiedAfter!=null ){
            query += " and modifiedTime > '%s'".formatted(DateTimeFormatter.ISO_INSTANT.format(modifiedAfter));
        }
        List<DriveFile> files = new ArrayList<>();
        String pageToken = null;
        do {
            FileList res = drive.files().list().setQ(query).setFields("nextPageToken, " + FIELDS)
                    .setPageToken(pageToken).execute();
            for (File file : res.getFiles()) {
                files.add(
                        new DriveFile(
                            file.getId(),
                            file.getName(),
                            file.getMimeType(),
                            Instant.ofEpochMilli(file.getModifiedTime().getValue())
                    )
                );
            }
            pageToken = res.getNextPageToken();
        } while (pageToken != null);
        return files;
    }

    public byte[] downloadFile(String fileId) throws IOException{
        try ( ByteArrayOutputStream out = new ByteArrayOutputStream() )
        {
            drive.files().get(fileId).executeMediaAndDownloadTo(out);
            return out.toByteArray();
        }

    }
}
