package de.steinhae.lab.lambda.services;

import java.io.ByteArrayInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;

import de.steinhae.lab.lambda.interfaces.BlobStorage;

public class AwsS3StorageHelper implements BlobStorage {

    private final String S3_PROTOCOL = "s3://";
    private final String HTTP = "http";

    private final Gson gson;
    private final AmazonS3 amazonS3;

    public AwsS3StorageHelper() {
        gson = new Gson();
        amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }

    @Override
    public void writeObjectToCloud(Object object, String filepath) {

        String json = gson.toJson(object);
        byte[] jsonBytes = json.getBytes();

        AmazonS3URI uri = parseS3URI(filepath);

        byte[] resultByte = DigestUtils.md5(jsonBytes);
        String streamMD5 = new String(Base64.encodeBase64(resultByte));

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(jsonBytes.length);
        objectMetadata.setContentMD5(streamMD5);

        ByteArrayInputStream dataInputStream = new ByteArrayInputStream(jsonBytes);

        PutObjectRequest putObjectRequest = new PutObjectRequest(
            uri.getBucket(),
            uri.getKey(),
            dataInputStream,
            objectMetadata);
        try {
            amazonS3.putObject(putObjectRequest);
        } catch (Exception e) {
            String targetPath = uri.getBucket() + "/" + uri.getKey();
            throw new RuntimeException("Error while writing string to cloud (target path " + targetPath + ").", e);
        }
    }

    private AmazonS3URI parseS3URI(String uri) {
        if (!uri.startsWith(HTTP)) {
            uri = uri.startsWith(S3_PROTOCOL) ? uri : S3_PROTOCOL + uri;
        }
        try {
            return new AmazonS3URI(uri);
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing " + uri + " as S3 URI.", e);
        }
    }
}
