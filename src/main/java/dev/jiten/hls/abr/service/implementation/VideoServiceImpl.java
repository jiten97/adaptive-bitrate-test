package dev.jiten.hls.abr.service.implementation;

import com.amazonaws.services.mediaconvert.model.CreateJobResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.UploadPartResult;
import dev.jiten.hls.abr.exceptionHandler.CustomException;
import dev.jiten.hls.abr.repository.DynamoDbRepository;
import dev.jiten.hls.abr.repository.S3Repository;
import dev.jiten.hls.abr.service.MediaConverterService;
import dev.jiten.hls.abr.service.VideoService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger logger = LogManager.getLogger(VideoServiceImpl.class);

    @Value("${amazon-properties.upload-bucket-folder}")
    private String uploadPath;

    @Autowired
    DynamoDbRepository dynamoDbRepository;
    @Autowired
    S3Repository s3Repository;
    @Autowired
    MediaConverterService mediaConverterService;

    public JSONObject uploadFile(MultipartFile file) throws Exception{
        JSONObject response = new JSONObject();
        String fileKey = UUID.randomUUID().toString();
        response.put("fileKey",fileKey);
        String path = uploadPath+"/"+fileKey;
        logger.info("Path created to upload file: {}",path);
        try{
            s3Repository.uploadObject(path,file);

            Pair<String,CreateJobResult> createJobResultPair = mediaConverterService.transcodingJobQueue(fileKey, path);

            JSONObject dynamoData = new JSONObject();
            dynamoData.put("FileKey", fileKey);
            dynamoData.put("FilePath", path);
            dynamoData.put("Status", "C");
            dynamoData.put("UploadId",JSONObject.NULL);
            dynamoData.put("CreatedOn", LocalDateTime.now().toString());
            dynamoData.put("TranscodingPath",createJobResultPair.getLeft());
            dynamoData.put("UpdatedOn", JSONObject.NULL);
            dynamoDbRepository.uploadItem(dynamoData);

            response.put("transcodeJobId",createJobResultPair.getRight().getJob().getId());

            return response;
        }catch (Exception e){
            logger.error("Exception while uploading file: "+e.getMessage());
            throw e;
        }
    }

    public JSONObject initiateFileUpload(JSONObject request) throws Exception{
        JSONObject response = new JSONObject();

        String fileKey = UUID.randomUUID().toString();
        response.put("fileKey",fileKey);
        String filePath = uploadPath+"/"+fileKey;
        logger.info("Path created to upload multipart file: {}",filePath);

        try{
            InitiateMultipartUploadResult initiateMultipartUploadResult = s3Repository.initiateMultipart(filePath,request.getString("contentType"),request.getString("fileName"));
            logger.info("Multipart request Initiated for key: {} with uploadId: {}",fileKey,initiateMultipartUploadResult.getUploadId());

            JSONObject dynamoData = new JSONObject();
            dynamoData.put("FileKey", fileKey);
            dynamoData.put("FilePath", filePath);
            dynamoData.put("Status", "P");
            dynamoData.put("UploadId",initiateMultipartUploadResult.getUploadId());
            dynamoData.put("CreatedOn", LocalDateTime.now().toString());
            dynamoData.put("TranscodingPath",JSONObject.NULL);
            dynamoData.put("UpdatedOn", JSONObject.NULL);
            dynamoDbRepository.uploadItem(dynamoData);
            logger.info("DynamoDB data is uploaded for file: {}",fileKey);

            response.put("uploadId",initiateMultipartUploadResult.getUploadId());
            return response;
        }catch (Exception e){
            logger.error("Exception while initiating multipart upload for file: {} with exception: {}",fileKey,e.getMessage());
            throw e;
        }
    }

    public JSONObject partFileUpload(String fileKey, MultipartFile file,Integer partNumber, Boolean isLast) throws Exception{
        JSONObject response = new JSONObject();
        logger.info("FileKey: {} for partRequest: {}",fileKey,partNumber);
        try{
            JSONObject dynamoData = getDynamoItem(fileKey);

            UploadPartResult uploadPartResult = s3Repository.uploadMultipart(dynamoData.getString("FilePath"),file,dynamoData.getString("UploadId"),partNumber,isLast);

            Integer eTagPartNumber = uploadPartResult.getPartNumber();
            String eTag = uploadPartResult.getETag();
            logger.info("Multipart Uploaded for part: {} and eTag: {}",eTagPartNumber,eTag);

            response.put("partNumber",eTagPartNumber);
            response.put("eTag",eTag);
            return response;
        }catch (Exception e){
            logger.error("Exception while part uploading multipart for: {} with exception: {}",fileKey,e.getMessage());
            throw e;
        }
    }

    public JSONObject completeFileUpload(JSONObject request) throws Exception{
        JSONObject response = new JSONObject();
        String fileKey = request.getString("fileKey");
        try {
            JSONObject dynamoData = getDynamoItem(fileKey);

            JSONArray eTagArray = request.getJSONArray("eTagDetailArray");
            List<Pair<Integer,String>> etagList = new ArrayList<>();
            for(int i=0;i<eTagArray.length();i++){
                JSONObject eTagDetails = eTagArray.getJSONObject(i);
                logger.info("PartDetails fetched: {}",eTagDetails.toString());
                etagList.add(Pair.of(eTagDetails.getInt("partNumber"),eTagDetails.getString("eTag")));
            }

            s3Repository.completeMultipart(dynamoData.getString("FilePath"),dynamoData.getString("UploadId"),etagList);
            logger.info("Multipart request completed for fileKey: {}",fileKey);

            Pair<String,CreateJobResult> createJobResultPair = mediaConverterService.transcodingJobQueue(fileKey, dynamoData.getString("FilePath"));
            logger.info("Media Conversion job is queued for jobId: {}",createJobResultPair.getRight().getJob().getId());

            dynamoDbRepository.updateItem(fileKey,createJobResultPair.getLeft(),"C");
            logger.info("DynamoDB status changed to complete for file: {}",fileKey);

            response.put("fileKey",fileKey);
            response.put("transcodeJobId",createJobResultPair.getRight().getJob().getId());
            response.put("transcodePath",createJobResultPair.getLeft());
            return response;
        }catch (Exception e){
            logger.error("Exception while completing multipart for file: {} with exception: {}",fileKey,e.getMessage());
            throw e;
        }
    }

    private JSONObject getDynamoItem(String fileKey) throws Exception{
        JSONObject dynamoData = dynamoDbRepository.getItem(fileKey);
        if(dynamoData.isEmpty() || !dynamoData.has("FileKey")){
            throw new CustomException("File Key is invalid",1004);
        }
        if(dynamoData.getString("Status").equalsIgnoreCase("C")){
            throw new CustomException("File is already processed",1005);
        }
        logger.info("Item received from dynamo: {}",dynamoData.toString());
        return dynamoData;
    }
}
