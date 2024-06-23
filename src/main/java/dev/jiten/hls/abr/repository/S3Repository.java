package dev.jiten.hls.abr.repository;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class S3Repository implements ApplicationRunner {

    private static final Logger logger = LogManager.getLogger(S3Repository.class);

    private AmazonS3Client amazonS3Client;

    @Value("${amazon-properties.upload-bucket-name}")
    private String uploadBucketName;
    @Value("${amazon-properties.access-key}")
    private String AWS_ACCESS_KEY;
    @Value("${amazon-properties.secret-key}")
    private String AWS_SECRET_KEY;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Initialising S3 client");
        this.amazonS3Client = (AmazonS3Client) AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .build();
    }

    public PutObjectResult uploadObject(String path, MultipartFile file) throws IOException {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setHeader(Headers.CONTENT_TYPE, file.getContentType());
            objectMetadata.setHeader(Headers.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.getOriginalFilename()).build().toString());
            PutObjectRequest putObjectRequest = new PutObjectRequest(uploadBucketName,path,file.getInputStream(),objectMetadata);
            return  amazonS3Client.putObject(putObjectRequest);
        }catch (Exception e){
            logger.error("Exception while Uploading File: {} with Exception: {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }
    public InitiateMultipartUploadResult initiateMultipart(String path, String contentType, String originalFileName) throws IOException {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setHeader(Headers.CONTENT_TYPE, contentType);
            objectMetadata.setHeader(Headers.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(originalFileName).build().toString());
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(uploadBucketName,path,objectMetadata);
            return amazonS3Client.initiateMultipartUpload(initiateMultipartUploadRequest);
        }catch (Exception e){
            logger.error("Exception while Initiating Multipart for: {} with Exception: {}", originalFileName, e.getMessage());
            throw e;
        }
    }
    public UploadPartResult uploadMultipart(String path, MultipartFile file, String uploadId, Integer partNumber, Boolean isLast) throws IOException {
        try {
            UploadPartRequest uploadPartRequest = new UploadPartRequest().withBucketName(uploadBucketName).withKey(path).withInputStream(file.getInputStream())
                    .withUploadId(uploadId).withPartNumber(partNumber).withPartSize(file.getSize()).withLastPart(isLast);

            return amazonS3Client.uploadPart(uploadPartRequest);
        }catch (Exception e){
            logger.error("Exception while Uploading part for id: {} and part: {} with Exception: {}",uploadId,partNumber,e.getMessage());
            throw e;
        }
    }
    public void completeMultipart(String path, String uploadId, List<Pair<Integer,String>> partETagKeyList) throws IOException {
        try {
            List<PartETag> partETagList = new ArrayList<>();
            for(Pair<Integer,String> partETagKey : partETagKeyList){
                PartETag partETag = new PartETag(partETagKey.getLeft(),partETagKey.getRight());
                partETagList.add(partETag);
            }

            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest().withBucketName(uploadBucketName).withKey(path)
                    .withUploadId(uploadId).withPartETags(partETagList);
            amazonS3Client.completeMultipartUpload(completeMultipartUploadRequest);
        }catch (Exception e){
            logger.error("Exception while Completing Multipart for : {} with Exception: {}",uploadId,e.getMessage());
            throw e;
        }
    }
}
