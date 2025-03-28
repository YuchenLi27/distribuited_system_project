package com.consumer;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SQSConsumer {
    private static final Logger logger = LogManager.getLogger(SQSConsumer.class);

    private static final String QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/179327391440/dsSQSqueue"; // Replace  queue URL
    private static final int THREAD_POOL_SIZE = 30;
    private static final ConcurrentHashMap<String, SqsMessageBody> messageMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SqsClient sqsClient = SqsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_WEST_2)
                .build();

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.create()) // Default credentials
                .build();
        // Create an SQS client
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            logger.info("new message received");

            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                logger.info("creating one new thread");
                executorService.submit(() -> receiveMessages(dynamoDbClient, sqsClient)); // Continuously receive messages
            }

            // Initiate a graceful shutdown after submitting all tasks
            executorService.shutdown();

            // Wait until all tasks have finished or until 1200 seconds have passed
            if (!executorService.awaitTermination(1200, TimeUnit.SECONDS)) {
                executorService.shutdownNow();  // Forcefully shutdown if tasks are taking too long
            }

            logger.info("MessageMap size is {}", messageMap.size());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void receiveMessages(DynamoDbClient dynamoDbClient, SqsClient sqsClient) {
        while (true) {
            try {
                logger.info("{} receiving message", Thread.currentThread().getName());
                ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20) // Long polling for 20 seconds
                        .build();
                ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveMessageRequest);
                List<Message> messages = receiveMessageResponse.messages();
                logger.info("{}{}", Thread.currentThread().getName(), receiveMessageResponse);

                if (messages.isEmpty()) {
                    logger.info("{} No messages received.", Thread.currentThread().getName());
                } else {
                    logger.info("{} Message received", Thread.currentThread().getName());
                    List<Message> messageList = new ArrayList<>(messages);
                    processMessage(dynamoDbClient, messageList);
                    deleteMessage(sqsClient, messages);
                }
            } catch (SqsException e) {
                logger.info("{} Error receiving messages: {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
    }

    private static void processMessage(DynamoDbClient dynamoDbClient, List<Message> messageList) {
        try {
            putMessageInDynamoDb(dynamoDbClient, messageList);
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage());
        }
    }

    private static void deleteMessage(SqsClient sqsClient, List<Message> messages) {
        if (messages.isEmpty()) return;
        try {
            List<DeleteMessageBatchRequestEntry> entries = messages.stream()
                    .map(message -> DeleteMessageBatchRequestEntry.builder()
                            .id(message.messageId())
                            .receiptHandle(message.receiptHandle())
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            DeleteMessageBatchRequest deleteRequest = DeleteMessageBatchRequest.builder()
                    .queueUrl(QUEUE_URL)
                    .entries(entries)
                    .build();

            sqsClient.deleteMessageBatch(deleteRequest);
            logger.info("Deleted {} messages in batch", messages.size());
        } catch (SqsException e) {
            logger.error("Error deleting messages in batch: {}", e.getMessage());
        }
    }

    private static void putMessageInDynamoDb(DynamoDbClient dynamoDbClient, List<Message> messageList) {

        List<WriteRequest> writeRequests = new ArrayList<>();
        Map<String, List<WriteRequest>> batchItems = new HashMap<>();
        for (Message message : messageList) {
            // Store the message into the thread-safe ConcurrentHashMap
            SqsMessageBody messageBody = new Gson().fromJson(message.body(), SqsMessageBody.class);
            logger.info("Message body is ready");
            Map<String, AttributeValue> itemValues = new HashMap<>();
            itemValues.put("skierId", AttributeValue.builder().s(String.valueOf(messageBody.getSkierId())).build());
            itemValues.put("seasonId", AttributeValue.builder().s(messageBody.getSeasonId()).build());
            itemValues.put("dayId", AttributeValue.builder().s(messageBody.getDayId()).build());
            itemValues.put("vertical",
                    AttributeValue.builder().n(String.valueOf(messageBody.getLiftRide().getLiftID() * 10)).build());
            itemValues.put("liftId",
                    AttributeValue.builder().s(String.valueOf(messageBody.getLiftRide().getLiftID())).build());
            itemValues.put("liftTime",
                    AttributeValue.builder().s(String.valueOf(messageBody.getLiftRide().getTime())).build());
            itemValues.put("resortId", AttributeValue.builder().s(String.valueOf(messageBody.getResortId())).build());

            WriteRequest writeRequest = WriteRequest.builder()
                    .putRequest(r -> r.item(itemValues))
                    .build();
            writeRequests.add(writeRequest);
        }

        batchItems.put("SkiResortData", new ArrayList<>(writeRequests));
        batchWrite(dynamoDbClient, batchItems);
    }

    private static void batchWrite(DynamoDbClient dynamoDbClient, Map<String, List<WriteRequest>> batchItems) {
        try {
            BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                    .requestItems(batchItems)
                    .build();
            dynamoDbClient.batchWriteItem(batchWriteItemRequest);
        } catch (DynamoDbException e) {
            logger.error("Batch write failed: " + e.getMessage());
        }
    }
}
