package com.sengled.mediaworker.s3;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.springframework.util.CollectionUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;

public class AmazonS3Template {
    private AmazonS3 client;

    public AmazonS3Template(AmazonS3 client) {
        this.client = client;
    }

    private AmazonS3 client() throws IOException {
        if (null == client) {
            throw new IOException("cant connect with S3");
        }
        return client;
    }

    public PutObjectResult putObject(String bucketName, String key, File src) throws IOException {
        try {
            return client().putObject(bucketName, key, src);
        } catch (AmazonClientException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public PutObjectResult putObject(String bucketName, String key, byte[] bytes) throws IOException {
        return putObject(bucketName, key, new ByteArrayInputStream(bytes), bytes.length);
    }

    public PutObjectResult putObject(String bucketName, String key, InputStream data, int length) throws IOException {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(length);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, data, objectMetadata);
            return client().putObject(putObjectRequest);
        } catch (AmazonClientException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(data);
        }
    }

    public PutObjectResult putObject(String bucketName, String key, byte[] bytes, List<Tag> tags) throws IOException {
        ByteArrayInputStream data = new ByteArrayInputStream(bytes);
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(bytes.length);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, data, objectMetadata);
            if( ! CollectionUtils.isEmpty(tags) ){
                putObjectRequest.setTagging(new ObjectTagging(tags));    
            }
            return client().putObject(putObjectRequest);
        } catch (AmazonClientException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(data);
        }
    }

    public S3Object getObject(String bucketName, String key) throws IOException {
        try {
            return client().getObject(bucketName, key);
        } catch (AmazonClientException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public InputStream getObjectInputStream(String bucketName, String key) throws IOException {
        try {
            S3Object object = client().getObject(bucketName, key);
            InputStream content = object.getObjectContent();

            return content instanceof AutoCloseInputStream ? content : new AutoCloseInputStream(content);
        } catch (AmazonClientException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public PutObjectResult putObject(PutObjectRequest putObjectRequest) throws IOException {
        try {
            return client().putObject(putObjectRequest);
        } catch (AmazonClientException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
