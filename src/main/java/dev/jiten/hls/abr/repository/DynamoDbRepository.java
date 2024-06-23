package dev.jiten.hls.abr.repository;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DynamoDbRepository implements ApplicationRunner {

    private static final Logger logger = LogManager.getLogger(DynamoDbRepository.class);

    private DynamoDB dynamoDB;

    @Value("${amazon-properties.dynamo-table-name}")
    private String dynamoTableName;
    @Value("${amazon-properties.access-key}")
    private String AWS_ACCESS_KEY;
    @Value("${amazon-properties.secret-key}")
    private String AWS_SECRET_KEY;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Initialising DynamoDB");
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .build());
    }

    public void uploadItem(JSONObject dynamoData){
        try {
            dynamoDB.getTable(dynamoTableName).putItem(Item.fromJSON(dynamoData.toString()));
        }catch (Exception e){
            logger.error("Exception while uploading file for: {} with Exception: {}",dynamoData.getString("FileKey"),e.getMessage());
            throw e;
        }
    }

    public JSONObject getItem(String fileKey){
        try {
            Item item = dynamoDB.getTable(dynamoTableName).getItem("FileKey",fileKey);
            return new JSONObject(item.toJSON());
        }catch (Exception e){
            logger.error("Exception while getting file for: {} with Exception: {}",fileKey,e.getMessage());
            throw e;
        }
    }

    public void updateItem(String fileKey, String transcodingPath, String status){
        try {
            List<AttributeUpdate> attributeUpdateList = new ArrayList<>();

            attributeUpdateList.add(new AttributeUpdate("TranscodingPath").put(transcodingPath));
            attributeUpdateList.add(new AttributeUpdate("UpdatedOn").put(LocalDateTime.now().toString()));
            attributeUpdateList.add(new AttributeUpdate("Status").put(status));

            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("FileKey",fileKey)
                    .withAttributeUpdate(attributeUpdateList);

            dynamoDB.getTable(dynamoTableName).updateItem(updateItemSpec);
        }catch (Exception e){
            logger.error("Exception while updating file for: {} with Exception: {}",fileKey,e.getMessage());
            throw e;
        }
    }
}
