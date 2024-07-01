package dev.jiten.hls.abr.service.implementation;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.mediaconvert.AWSMediaConvertClient;
import com.amazonaws.services.mediaconvert.AWSMediaConvertClientBuilder;
import com.amazonaws.services.mediaconvert.model.*;
import dev.jiten.hls.abr.exceptionHandler.CustomException;
import dev.jiten.hls.abr.service.MediaConverterService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MediaConverterServiceImpl implements MediaConverterService, ApplicationRunner {

    private static final Logger logger = LogManager.getLogger(MediaConverterServiceImpl.class);

    private AWSMediaConvertClient awsMediaConvertClient;

    @Value("${amazon-properties.upload-bucket-name}")
    private String uploadBucketName;
    @Value("${amazon-properties.transcode-bucket-name}")
    private String transcodeBucketName;
    @Value("${amazon-properties.transcode-bucket-folder}")
    private String transcodeFolderPath;
    @Value("${amazon-properties.mediaConverter-role}")
    private String mediaConverterRole;
    @Value("${amazon-properties.mediaConverter-template-name}")
    private String mediaConverterTemplateName;
    @Value("${amazon-properties.access-key}")
    private String AWS_ACCESS_KEY;
    @Value("${amazon-properties.secret-key}")
    private String AWS_SECRET_KEY;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Initialising MediaConverter client");
        this.awsMediaConvertClient = (AWSMediaConvertClient) AWSMediaConvertClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .build();
    }

    public Pair<String,CreateJobResult> transcodingJobQueue(String fileKey, String filePath){
        try {
            JobSettings jobSettings = new JobSettings();
            String inputPath = "s3://" + uploadBucketName + "/" + filePath;
            logger.info("Input path created: {}", inputPath);

            Input jobInput = new Input().withFileInput(inputPath);
            jobSettings.setInputs(List.of(jobInput));

            OutputGroupSettings outputGroupSettings = new OutputGroupSettings();

            String transcodePath = transcodeFolderPath + "/" + fileKey + "/index";
            logger.info("TransCode path created: {}", transcodePath);
            HlsGroupSettings hlsGroupSettings = new HlsGroupSettings()
                    .withDestination("s3://"+transcodeBucketName+"/"+transcodePath);
            outputGroupSettings.withHlsGroupSettings(hlsGroupSettings);

            jobSettings.setOutputGroups(List.of(new OutputGroup().withOutputGroupSettings(outputGroupSettings)));
            CreateJobRequest createJobRequest = new CreateJobRequest().withJobTemplate(mediaConverterTemplateName)
                    .withSettings(jobSettings)
                    .withRole(mediaConverterRole);

            return Pair.of(transcodePath+".m3u8",awsMediaConvertClient.createJob(createJobRequest));
        }catch (Exception e){
            logger.error("Exception while creating job for: {} with Exception: {}",fileKey,e.getMessage());
            throw e;
        }
    }

    public String checkingJobStatus(String jobId) throws CustomException {
        GetJobRequest getJobRequest = new GetJobRequest().withId(jobId);
        GetJobResult getJobResult = awsMediaConvertClient.getJob(getJobRequest);
        if(getJobResult==null || getJobResult.getJob()==null || getJobResult.getJob().getStatus()==null || getJobResult.getJob().getStatus().isEmpty()){
            throw new CustomException("Wrong Job Id Provided",1004);
        }
        logger.info("Job status: {} for JobId: {}",getJobResult.getJob().getStatus(),jobId);
        return getJobResult.getJob().getStatus();
    }
}
