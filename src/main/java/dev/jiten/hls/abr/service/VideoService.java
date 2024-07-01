package dev.jiten.hls.abr.service;

import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {
    public JSONObject uploadFile(MultipartFile file) throws Exception;
    public JSONObject initiateFileUpload(JSONObject request) throws Exception;
    public JSONObject partFileUpload(String fileKey, MultipartFile file,Integer partNumber, Boolean isLast) throws Exception;
    public JSONObject completeFileUpload(JSONObject request) throws Exception;
    public JSONObject videoUrl(String fileKey) throws Exception;
}
