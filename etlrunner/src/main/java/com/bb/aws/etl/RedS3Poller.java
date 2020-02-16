package com.bb.aws.etl;

import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedS3Poller implements RequestHandler<String, String> {

    private final static String RED_S3_LOC = "s3://serverlessdatalakepocforeqrs/red/dvdrental/";
    private final static String queueName = "dvds3queue";
    private final static String rentalPrefix = "red/dvdrental/public/rental/";
    private final static String paymentPrefix = "red/dvdrental/public/payment/";
    private final static String customerPrefix = "red/dvdrental/public/customer/";
    private final static String GREEN_S3_LOC = "s3://serverlessdatalakepocforeqrs/green/dvdrental/";


    public String handleRequest(String s, Context context) {
        final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        final String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
        final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        System.out.println("Receiving messages from queue:" + queueName);

        try {
            List<String> keys = new ArrayList<String>();

            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            while (messages.size() > 0) {
                List<String> receiptHandles = processMessages(messages, keys);
                deleteMessages(sqs, queueUrl, receiptHandles);
                messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            }
            callGlue(keys);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "done";
    }

    private void callGlue(List<String> keys) {
        GlueJobContext context = new GlueJobContext();
        for(String key:keys) {
            System.out.println("Acquired the key:" + key);
            if (key.startsWith(rentalPrefix)) {
                context.setRentalFileLoc(key);
            } else if (key.startsWith(customerPrefix)) {
                context.setCustomerFileLoc(key);
            } else if (key.startsWith(paymentPrefix)) {
                context.setPaymentFileLoc(key);
            } else {
                System.out.println("No matching key found for " + key);
            }
        }
        context.setS3OutputLoc(GREEN_S3_LOC);

        final GlueClient glueClient = LambdaInvokerFactory.builder()
                .lambdaClient(AWSLambdaClientBuilder.defaultClient())
                .build(GlueClient.class);

        glueClient.run(context);
    }

    private List<String>  processMessages(List<Message> messages, List<String> keys) {
        System.out.println("There are " + messages.size() + " messages");
        List<String> receiptHandles = new ArrayList<String>();
        for (final Message message : messages) {
            System.out.println("Message");
            System.out.println("  MessageId:     " + message.getMessageId());
            System.out.println("  ReceiptHandle: " + message.getReceiptHandle());
            System.out.println("  MD5OfBody:     " + message.getMD5OfBody());
            System.out.println("  Body:          " + message.getBody());
            for (final Map.Entry<String, String> entry : message.getAttributes().entrySet()) {
                System.out.println("Attribute");
                System.out.println("  Name:  " + entry.getKey());
                System.out.println("  Value: " + entry.getValue());
            }
            processS3Event(message.getBody(), keys);
            receiptHandles.add(message.getReceiptHandle());
        }
        return receiptHandles;
    }

    private void processS3Event(String messageBody, List<String> keys) {
        S3EventNotification s3EventNotification = S3EventNotification.parseJson(messageBody);
        System.out.println("Number of records in the S3 event notification:" + s3EventNotification.getRecords().size());
        for(S3EventNotification.S3EventNotificationRecord record:s3EventNotification.getRecords()) {
            String key = record.getS3().getObject().getKey();
            System.out.println("Adding key:" + key);
            keys.add(key);
        }
    }

    private void deleteMessages(AmazonSQS sqs, String queueUrl, List<String> receiptHandles) {
        // Delete the message
        for(String handle:receiptHandles) {
            System.out.println("Deleting the message with handle:" + handle);
            sqs.deleteMessage(new DeleteMessageRequest(queueUrl, handle));
        }
    }


}
