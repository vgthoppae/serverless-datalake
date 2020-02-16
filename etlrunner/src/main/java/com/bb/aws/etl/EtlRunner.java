package com.bb.aws.etl;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EtlRunner implements RequestHandler<S3Event, String> {

    private static final Logger log = LogManager.getLogger(EtlRunner.class);

    private static final Map<String, String> ACTIVITY_ARN_MAP = new HashMap<String, String>();

    static {
        ACTIVITY_ARN_MAP.put("external/dummy.txt", "arn:aws:states:us-east-1:XXXX:activity:ReceiveIDRExtractActivity");
    }

    public String handleRequest(S3Event input, Context context) {
        try {
            // Get Event Record
            S3EventNotification.S3EventNotificationRecord record = input.getRecords().get(0);

            // Source File Name
            String srcFileName = record.getS3().getObject().getKey();

            String eventName = record.getEventName();

            log.info("Event {} on {}", eventName, srcFileName);

            if (!ACTIVITY_ARN_MAP.containsKey(srcFileName)) {
                log.info("No runner configured for key {}", srcFileName);
            } else {
                String activity_arn = ACTIVITY_ARN_MAP.get(srcFileName);
                log.info("Notifying activity {}", activity_arn);
                performActivity(activity_arn);
                log.info("Acitivity {} completed", activity_arn);
            }
            return "success";
        } catch (Exception e) {
            log.error(e);
        }

        return null;
    }

    public String makeJson(String result) throws Exception {
        return "{\"Result\": \"" + result + "\"}";
    }

    private void performActivity(String activity_arn) throws Exception {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSocketTimeout((int) TimeUnit.SECONDS.toMillis(70));

        AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();

        while (true) {
            GetActivityTaskResult getActivityTaskResult = client
                    .getActivityTask(new GetActivityTaskRequest().withActivityArn(activity_arn));

            if (getActivityTaskResult.getTaskToken() != null) {
                try {
                    String result = "File is received";
                    JsonNode json = Jackson.jsonNodeOf(getActivityTaskResult.getInput());
                    // String greetingResult =
                    // getGreeting(json.get("who").textValue());
                    client.sendTaskSuccess(new SendTaskSuccessRequest().withOutput(makeJson(result))
                            .withTaskToken(getActivityTaskResult.getTaskToken()));
                    log.info("Activity Task execution complete......");
                    return;
                } catch (Exception e) {
                    client.sendTaskFailure(
                            new SendTaskFailureRequest().withTaskToken(getActivityTaskResult.getTaskToken()));
                    log.error("Activity Task execution failed......");
                    log.error(e);
                    return;
                }
            } else {
                log.info("Activy {} is not available - will try again in 5 seconds", activity_arn);
                Thread.sleep(5000);
            }
        }
    }
}