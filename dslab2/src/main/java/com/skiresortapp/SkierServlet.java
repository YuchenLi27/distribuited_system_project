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
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private static final String QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/179327391440/dsSQSqueue";
    private static final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();

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

            String resort = request.getParameter("resort");
            String season = request.getParameter("seasonId");

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

            response.setStatus(HttpServletResponse.SC_OK);
            // TODO: update the total vertical for response
            Integer totalVertical = 34507;
            response.getWriter().write(new Gson().toJson(totalVertical));
            response.getWriter().flush();

        } else if (urlParts.length == 3 && urlParts[2].equals("vertical")) {
            // GET /skiers/{skierID}/vertical:
            // get the total vertical for the skier the specified resort. If no season is specified, return all seasons
            try {
                int skierID = Integer.parseInt(urlParts[1]);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid skier ID.");
                return;
            }

            // TODO: Get the vertical records of skier for resorts and build corresponding SkierVerticalRecords response
            SkierVerticalRecord dummyRecord = new SkierVerticalRecord("2018", 33333);
            SkierVerticalRecords dummyResponse = new SkierVerticalRecords(List.of(dummyRecord));

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(new Gson().toJson(dummyResponse));
            response.getWriter().flush();
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

