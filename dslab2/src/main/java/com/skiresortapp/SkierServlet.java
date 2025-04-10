package com.skiresortapp;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.skiresortapp.bean.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;


@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private static final String QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/179327391440/dsSQSqueue";
    private static final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
    private static final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_WEST_2)
            .build();


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        String urlPath = request.getPathInfo();


        // check if we have a URL
        if (urlPath == null || urlPath.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error form check url path: missing parameters.");
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (urlParts.length == 8) {
            // GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}:
            // get the total vertical for the skier for the specified ski day
            if (!isValidUrl(urlParts)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error from url: Invalid URL.");
                return;
            }
            if (!isValidParameter(urlParts)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error from parameter: Invalid parameters.");
                return;
            }

            String resort = urlParts[1];
            String season = urlParts[3];

            if (resort == null || resort.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Missing resort parameter.");
                return;
            } else if (season == null || season.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Missing season parameter.");
                return;
            }
            try {
                int seasonID = Integer.parseInt(season);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid season parameter.");
                return;
            }
            try{
                int resortID = Integer.parseInt(resort);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid resort parameter.");
                return;
            }

            // TODO: update the total vertical for response
            Map<String, AttributeValue> keyToGet = new HashMap<>();
            String skierID = urlParts[7];
            int dayID = Integer.parseInt(urlParts[5]);

            keyToGet.put(":skierId", AttributeValue.fromS(skierID));
            keyToGet.put(":dayId", AttributeValue.fromS(String.valueOf(dayID)));

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName("SkiResortData")
                    .keyConditionExpression("skierId = :skierId AND dayId = :dayId")
                    .expressionAttributeValues(keyToGet)
                    .build();

            QueryResponse dynamodbResponse = ddb.query(queryRequest);

            try {
                int totalVertical = 0;
                for (Map<String, AttributeValue> item : dynamodbResponse.items()) {
                    if (item.containsKey("vertical")) {
                        totalVertical += Integer.parseInt(item.get("vertical").n());
                    }
                }
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(new Gson().toJson(totalVertical));
                response.getWriter().flush();

            } catch (DynamoDbException e) {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }

        } else if (urlParts.length == 3 && urlParts[2].equals("vertical")) {
            // GET /skiers/{skierID}/vertical:
            // get the total vertical for the skier the specified resort. If no season is specified, return all seasons

            // TODO: Get the vertical records of skier for resorts and build corresponding SkierVerticalRecords response

            String skierId = urlParts[1];
            if (skierId == null || skierId.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Missing skierID parameter.");
                return;
            }

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":skierId", AttributeValue.builder().s(skierId).build());

            try {
                QueryRequest queryRequest = QueryRequest.builder()
                        .tableName("SkiResortData")
                        .keyConditionExpression("skierId = :skierId")
                        .expressionAttributeValues(expressionValues)
                        .build();

                QueryResponse dynamodbResponse = ddb.query(queryRequest);

                //to store total vertical per resort and season
                Map<String, Map<String, Integer>> resortVerticalMap = new HashMap<>();

                for (Map<String, AttributeValue> item : dynamodbResponse.items()) {
                    String resortId = item.get("resortId").s();
                    String seasonId = item.get("seasonId").s();
                    int vertical = Integer.parseInt(item.get("vertical").n());

                    // Update the total vertical for the specific resort and season
                    resortVerticalMap
                            .computeIfAbsent(resortId, k -> new HashMap<>())
                            .merge(seasonId, vertical, Integer::sum);
                }
                List<Map<String, Object>> resorts = new ArrayList<>();

                for (Map.Entry<String, Map<String, Integer>> resortEntry : resortVerticalMap.entrySet()) {
                    String resortId = resortEntry.getKey();
                    Map<String, Integer> seasonsMap = resortEntry.getValue();

                    for (Map.Entry<String, Integer> seasonEntry : seasonsMap.entrySet()) {
                        String seasonId = seasonEntry.getKey();
                        int totalVert = seasonEntry.getValue();

                        // Build a resort response
                        Map<String, Object> resortResponse = new HashMap<>();
                        resortResponse.put("seasonID", seasonId);
                        resortResponse.put("totalVert", totalVert);
                        resortResponse.put("resortID", resortId);
                        resorts.add(resortResponse);
                    }
                }

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("resorts", resorts);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(new Gson().toJson(responseData));
                response.getWriter().flush();

            } catch (DynamoDbException e) {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "URL not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // POST /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}:
        // Stores new lift ride details in the data store
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String urlPath = request.getPathInfo();

        // check if we have a URL
        if (urlPath == null || urlPath.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error from check url path: missing parameters.");
            return;
        }

        String[] urlParts = urlPath.split("/");

        // validate the URL path
        if (!isValidUrl(urlParts)) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error from url: Invalid URL.");
            return;
        }
        if (!isValidParameter(urlParts)) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error from parameter: Invalid parameters.");
            return;
        }

        StringBuilder requestBody = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;

        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        if (!isValidRideJson(requestBody.toString())) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Request Body");
            return;
        }

        int resortID = Integer.parseInt(urlParts[1]);
        String seasonID = urlParts[3];
        String dayID = urlParts[5];
        int skierID = Integer.parseInt(urlParts[7]);
        LiftRide liftRide;

        try {
            liftRide = new Gson().fromJson(requestBody.toString(), LiftRide.class);
        } catch (JsonSyntaxException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Request Body");
            return;
        }

        SqsMessageBody sqsMessageBody = new SqsMessageBody(resortID, seasonID, dayID,skierID, liftRide);
        String messageBody = new Gson().toJson(sqsMessageBody);
        sendMessageToSQS(messageBody);

        // TODO: store lift ride for corresponding resort, season, day, and skier.
        response.setStatus(HttpServletResponse.SC_CREATED);
        // There are no response body according to the Swagger spec for this POST request.
        response.getWriter().flush();
    }
    private void sendMessageToSQS(String messageBody){
        try{
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(QUEUE_URL)
                    .withMessageBody(messageBody);
            sqsClient.sendMessage(sendMessageRequest);
            System.out.println("Message sent to SQS" + messageBody);
        } catch (Exception e) {
            System.out.println("Error while sending message: " + e.getMessage());
        }
    }

    private boolean isValidUrl(String[] urlPath) {
        //url = /skiers/{resortID}/seasons/{seasonID}/days/{dayID}
        return urlPath.length == 8
                && urlPath[2].equals("seasons")
                && urlPath[4].equals("days")
                && urlPath[6].equals("skiers");
    }

    private boolean isValidParameter(String[] urlPath) {
        try {
            int resortID = Integer.parseInt(urlPath[1]);
            int seasonID = Integer.parseInt(urlPath[3]);
            int dayID = Integer.parseInt(urlPath[5]);
            int skierID = Integer.parseInt(urlPath[7]);
            return 1 <= dayID && dayID <= 366;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidRideJson(String requestBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            if (jsonObject.size() != 2 || !jsonObject.has("time") || !jsonObject.has("liftID")) {
                return false;
            }
            try {
                int time = jsonObject.get("time").getAsInt();
                int liftID = jsonObject.get("liftID").getAsInt();
            } catch (NumberFormatException e) {
                return false;
            }
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(new Gson().toJson(new ErrorMessage(message)));
        response.getWriter().flush();
    }

}

