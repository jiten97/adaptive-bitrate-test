package dev.jiten.hls.abr.controller;

import dev.jiten.hls.abr.exceptionHandler.CustomException;
import dev.jiten.hls.abr.service.MediaConverterService;
import dev.jiten.hls.abr.service.VideoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(maxAge = 3600,allowedHeaders = {"Content-Type"},origins = {"*"},methods = {RequestMethod.POST,RequestMethod.GET})
@RequestMapping("/video")
public class VideoController {

    private static final Logger logger = LogManager.getLogger(VideoController.class);

    @Autowired
    VideoService videoService;
    @Autowired
    MediaConverterService mediaConverterService;

    @PostMapping(value = "/single/v1/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadVideo(@RequestPart("file")MultipartFile multipartFile) throws Exception{
        logger.info("Inside File Upload Request");
        logger.info("FileName: "+multipartFile.getOriginalFilename());
        logger.info("FileSize: "+multipartFile.getSize());
        logger.info("Content-Type: "+multipartFile.getContentType());
        JSONObject response = videoService.uploadFile(multipartFile);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

    @PostMapping(value = "/multipart/v1/initiate",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> initiateMultipart(@RequestBody Object object) throws Exception {
        logger.info("Inside Multipart File Upload Initiation");
        JSONObject request = (JSONObject) JSONObject.wrap(object);

        String[] initiateRequestedFields = new String[]{"fileName","fileSize","contentType"};
        for(String s:initiateRequestedFields){
            if(!request.has(s)){
                throw new CustomException("Requested Field: "+s+" to initiate Upload missing",1001);
            }
        }

        logger.info("FileName: {}", request.getString("fileName"));
        logger.info("FileSize: {}",request.getLong("fileSize"));
        logger.info("Content-Type: {}", request.getString("contentType"));

        JSONObject response = videoService.initiateFileUpload(request);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }
    @PostMapping(value = "/multipart/v1/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadMultipart(@RequestPart("file")MultipartFile multipartFile,
                                                  @RequestPart("fileKey") String fileKey,
                                                  @RequestPart("partNumber") String partNumber,
                                                  @RequestPart("isLast") String isLast) throws Exception{
        logger.info("Inside Multipart File Upload Request");
        logger.info("FileKey: {}", fileKey);
        JSONObject response = videoService.partFileUpload(fileKey,multipartFile,Integer.valueOf(partNumber),isLast.equalsIgnoreCase("true"));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }
    @PostMapping(value = "/multipart/v1/complete",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> completeMultipart(@RequestBody Object object) throws Exception{
        logger.info("Inside Multipart File Upload Completion");
        JSONObject jsonObject = (JSONObject) JSONObject.wrap(object);
        String[] requestedFields = new String[]{"fileKey","eTagDetailArray"};
        for(String s:requestedFields){
            if(!jsonObject.has(s) || (s.equalsIgnoreCase("eTagDetailArray") && jsonObject.getJSONArray(s).isEmpty())){
                throw new CustomException("Requested Field: "+s+" to complete Upload missing",1002);
            }
        }
        logger.info("FileKey: {}", jsonObject.getString("fileKey"));
        JSONObject response = videoService.completeFileUpload(jsonObject);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }
    @GetMapping(value = "/transcoding/v1/status/{transcodingId}")
    public ResponseEntity<String> checkTranscodingStatus(@PathVariable("transcodingId") String transcodeJobId) throws CustomException {
        logger.info("Checking Transcoding Job Status for: {}",transcodeJobId);
        JSONObject statusResponse = new JSONObject();
        statusResponse.put("status",mediaConverterService.checkingJobStatus(transcodeJobId));
        return ResponseEntity.ok(statusResponse.toString());
    }
}
