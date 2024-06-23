package dev.jiten.hls.abr.service;

import com.amazonaws.services.mediaconvert.model.CreateJobResult;
import dev.jiten.hls.abr.exceptionHandler.CustomException;
import org.apache.commons.lang3.tuple.Pair;

public interface MediaConverterService {
    public Pair<String,CreateJobResult> transcodingJobQueue(String fileKey, String filePath);
    public String checkingJobStatus(String jobId) throws CustomException;
}