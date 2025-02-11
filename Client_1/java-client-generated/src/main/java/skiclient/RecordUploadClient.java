package skiclient;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

public class RecordUploadClient {
    private SkiersApi skiersApi;


    public RecordUploadClient(SkiersApi skiersApiClient) {
        this.skiersApi = skiersApiClient;
    }

    public int uploadLift(UploadEvent uploadEvent){
        int resortId = uploadEvent.getResortID();
        int skierId = uploadEvent.getSkierID();
        String seasonId = String.valueOf(uploadEvent.getSeasonID());
        String dayId = String.valueOf(uploadEvent.getDayID());
        LiftRide liftRide = new LiftRide().liftID(uploadEvent.getLiftID()).time(uploadEvent.getTime());

        return executeRequestWithRetry(1, liftRide, resortId, skierId, seasonId, dayId);

    }
    // client 1: handling errors
    private int executeRequestWithRetry(int times, LiftRide liftRide, int resortId, int skierId, String seasonId, String dayId){

        if (times > 5){
            return 0;
        }
        try {
            ApiResponse<Void> response = this.skiersApi.writeNewLiftRideWithHttpInfo(liftRide, resortId, seasonId, dayId, skierId);
            int httpStatusCodeCategory = response.getStatusCode();

            if (httpStatusCodeCategory == 400 || httpStatusCodeCategory == 500){
                executeRequestWithRetry(times + 1, liftRide, resortId, skierId, seasonId, dayId);
            }
        } catch (ApiException e) {
            executeRequestWithRetry(times + 1, liftRide, resortId, skierId, seasonId, dayId);

        }
        return 1;
    }
}
