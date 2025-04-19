package com.blinker.atom.util;

import com.blinker.atom.domain.appuser.AppUser;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.Channels;

@Slf4j
@Component
public class GCSUtil {
    @Value("${gcs.bucket.name}")
    private String bucketName;
    private final Storage storage;

    public GCSUtil(@Value("${gcs.credentials.path}") String credentialsPath) throws IOException {
        InputStream keyFile;
        if (credentialsPath.startsWith("classpath:")) {
            keyFile = new ClassPathResource(credentialsPath.replace("classpath:", "")).getInputStream();
        } else if (credentialsPath.startsWith("/") || credentialsPath.startsWith("file:")) {
            keyFile = new FileInputStream(credentialsPath);
        } else {
            keyFile = new ClassPathResource(credentialsPath).getInputStream();
        }
        storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(keyFile))
                .build().getService();
    }

    public String uploadFileToGCS(String directory, String filename, InputStream inputStream, String contentType) throws IOException {
        String filePath = (directory != null ? directory + "/" : "") + filename;
        storage.create(
                BlobInfo.newBuilder(bucketName, filePath)
                        .setContentType(contentType)
                        .setContentDisposition("inline")
                        .build(),
                inputStream
        );
        return "https://storage.googleapis.com/" + bucketName + "/" + filePath;
    }

    public InputStream downloadFileFromGCS(String objectPath) throws FileNotFoundException {
        Blob blob = storage.get(BlobId.of(bucketName, objectPath));
        log.info("GCS object path: {}", blob);

        if (blob == null || !blob.exists()) {
            throw new FileNotFoundException("GCS에 해당 파일이 존재하지 않습니다: " + objectPath);
        }

        return new ByteArrayInputStream(blob.getContent());
    }


}
