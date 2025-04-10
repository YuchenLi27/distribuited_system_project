package com.skiresortapp;

import com.google.gson.Gson;
import com.skiresortapp.bean.ErrorMessage;
import com.skiresortapp.bean.ResortResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@WebServlet(value = "/resorts/*")
public class ResortsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        String urlPath = request.getPathInfo();


        if (urlPath == null || urlPath.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error form check url path: missing parameters.");
            return;
        }
        String[] urlParts = urlPath.split("/");

        if (urlParts.length == 7){
            if (!isValidUrl(urlParts)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error from url: Invalid URL.");
                return;
            }
            if (!isValidParameter(urlParts)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Error from parameter: Invalid parameters.");
                return;
            }

            Map<String, AttributeValue> keyToGet = new HashMap<>();
            int resortId = Integer.parseInt(urlParts[1]);
            int seasonId = Integer.parseInt(urlParts[3]);
            int dayId = Integer.parseInt(urlParts[5]);

            keyToGet.put(":resortId", AttributeValue.fromS(String.valueOf(resortId)));
            keyToGet.put(":dayId", AttributeValue.fromS(String.valueOf(dayId)));
            keyToGet.put(":seasonId", AttributeValue.fromS(String.valueOf(seasonId)));

            DynamoDbClient dynamoDbClient = DynamoDbClient.create();
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName("SkiResortData")
                    .filterExpression("resortId = :resortId AND seasonId = :seasonId AND dayId = :dayId")
                    .expressionAttributeValues(Map.of(
                            ":resortId", AttributeValue.fromS(String.valueOf(resortId)),
                            ":seasonId", AttributeValue.fromS(String.valueOf(seasonId)),
                            ":dayId", AttributeValue.fromS(String.valueOf(dayId))
                    ))
                    .build();

            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

            Set<String> uniqueSkiers = new HashSet<>();
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                uniqueSkiers.add(item.get("skierId").s());
            }
            int count = uniqueSkiers.size();
            ResortResponse result = new ResortResponse(count);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(new Gson().toJson(result));
            response.getWriter().flush();

        }
    }

    private boolean isValidUrl(String[] urlPath) {
        //url = resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
        return urlPath.length == 7
                && urlPath[2].equals("seasons")
                && urlPath[4].equals("day")
                && urlPath[6].equals("skiers");
    }

    private boolean isValidParameter(String[] urlPath) {
        try {
            int resortID = Integer.parseInt(urlPath[1]);
            int seasonID = Integer.parseInt(urlPath[3]);
            int dayID = Integer.parseInt(urlPath[5]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(new Gson().toJson(new ErrorMessage(message)));
        response.getWriter().flush();
    }



}
