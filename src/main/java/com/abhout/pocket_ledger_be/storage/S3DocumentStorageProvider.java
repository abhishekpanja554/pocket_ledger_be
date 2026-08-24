package com.abhout.pocket_ledger_be.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3DocumentStorageProvider implements DocumentStorageProvider{
    private final String bucketName;
    private final S3Client s3Client;
    public S3DocumentStorageProvider(
            S3Client client,
            @Value("${app.storage.s3.bucket}") String bucketName
    ) {
        this.s3Client = client;
        this.bucketName = bucketName;
    }
    @Override
    public void put(String key, byte[] content, String type) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).contentType(type).build(),
                RequestBody.fromBytes(content)
        );
    }

    @Override
    public byte[] get(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                    .asByteArray();
        } catch (NoSuchKeyException e) {
            throw new StorageObjectNotFoundException();
        }
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    }
}
