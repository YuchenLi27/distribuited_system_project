import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import skiersortapp.ErrorMessages;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import skiersortapp.LiftRide;
import skiersortapp.SkierVerticalRecord;


@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        res.setCharacterEncoding("UTF-8");
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Error form check url path: missing parameters.");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)
        if (urlParts.length == 8) {
            // GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}:
            // get the total vertical for the skier for the specified ski day
            if (!isUrlValid(urlParts)) {
                sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Error from url: Invalid URL.");
                return;
            }
            if (!isValidParameter(urlParts)) {
                sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Error from parameter: Invalid parameters.");
                return;
            }
            res.setStatus(HttpServletResponse.SC_OK);
            String returnJson = "It works!";
            res.getWriter().write(new Gson().toJson(returnJson));


        } else if (urlParts.length == 3 && urlParts[2].equals("vertical")) {
            // /skiers/{skierID}/vertical
            try{
                int skierID = Integer.parseInt(urlParts[1]);
            } catch (NumberFormatException e) {
                sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid skierID.");
                return;
            }
            String resort = req.getParameter("resort");
            String season = req.getParameter("season");

            if (resort == null || resort.isEmpty()) {
                sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Missing resort parameter.");
                return;
            } else if (season != null){
                try{
                    int seasonID = Integer.parseInt(season);
                } catch (NumberFormatException e) {
                    sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid season parameter.");
                    return;
                }
//              to get the season id and total vertical (/skiers/{skierID}/vertical)


            }
            res.setStatus(HttpServletResponse.SC_OK);
            String returnJson = "It works!";
            res.getWriter().write(new Gson().toJson(returnJson));
        } else {
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL.");
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        String urlPath = req.getPathInfo();

        // check if we have a URL
        if (urlPath == null || urlPath.isEmpty()) {
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Error from check url path: missing parameters.");
            return;
        }

        String[] urlParts = urlPath.split("/");

        // validate the URL path
        if (!isUrlValid(urlParts)) {
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Error from url: Invalid URL.");
            return;
        }
        if (!isUrlValid(urlParts)){
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Error from parameter: Invalid parameters.");
            return;
        }

        StringBuilder requestBody = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;

        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        if (!isValidRideJson(requestBody.toString())) {
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid Request Body.");
            return;
        }
        int resortID = Integer.parseInt(urlParts[1]);
        int seasonID = Integer.parseInt(urlParts[3]);
        int dayID = Integer.parseInt(urlParts[5]);
        int skierID = Integer.parseInt(urlParts[7]);
        LiftRide lifeRide = new Gson().fromJson(requestBody.toString(), LiftRide.class);

        try {
            LiftRide liftRide = new Gson().fromJson(requestBody.toString(), LiftRide.class);
        } catch(JsonSyntaxException e){
            sendErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid LiftRide Body.");
        }

        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().flush();



    }
    private boolean isValidParameter(String[] urlPath) {
        // TODO: validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        try{
            int resortID = Integer.parseInt(urlPath[1]);
            int seasonID = Integer.parseInt(urlPath[3]);
            int dayID = Integer.parseInt(urlPath[5]);
            int skierID = Integer.parseInt(urlPath[7]);
            return 1 <= dayID && dayID <= 366;
        } catch (NumberFormatException e){
            return false;
        }

    }
    private boolean isUrlValid(String[] urlPath) {
        return urlPath.length == 8
                && urlPath[2].equals("season")
                && urlPath[4].equals("days")
                && urlPath[6].equals("skiers");

    }

    private boolean isValidRideJson(String requestBody){
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
        private void sendErrorMessage(HttpServletResponse res, int status, String message) throws IOException {
            res.setStatus(status);
            res.getWriter().write(new Gson().toJson(new ErrorMessages(message)));
            res.getWriter().flush();
        }

}
 
