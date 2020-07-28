package de.steinhae.lab.lambda.interfaces;

public interface BlobStorage {
    void writeObjectToCloud(Object object, String filepath);
}
