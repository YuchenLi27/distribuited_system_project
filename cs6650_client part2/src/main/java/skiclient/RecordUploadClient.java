package skiclient;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.BlockingQueue;

public class RecordUploadClient {
    private SkiersApi skiersApi;
    private BlockingQueue<LatencyRecord> latencyRecordQueue;

    public RecordUploadClient(SkiersApi skiersApiClient) {
        this.skiersApi = skiersApiClient;
    }

    public void setLatencyRecordQueue(BlockingQueue<LatencyRecord> latencyRecordQueue) {
        this.latencyRecordQueue = latencyRecordQueue;
    }

    public int uploadLift(UploadEvent uploadEvent) {
        int resortId = uploadEvent.getResortID();
        String seasonId = String.valueOf(uploadEvent.getSeasonID());
        String dayId = String.valueOf(uploadEvent.getDayID());
        int skierId = uploadEvent.getSkierID();
        LiftRide liftRide = new LiftRide()
                .liftID(uploadEvent.getLiftID())
                .time(uploadEvent.getTime());

        return executeRequestWithRetry(1, liftRide, resortId, seasonId, dayId, skierId);
    }

    /**
     * Upload a lift event with retry up to 5 times
     */
    private int executeRequestWithRetry(int times,
                                         LiftRide liftRide,
                                         int resortId,
                                         String seasonId,
                                         String dayId,
                                         int skierId) {
        if (times > 5) {
            return 0;
        }
        try {
            // Building the Client (Part 2)
            long startTime = System.nanoTime();

            ApiResponse<Void> response = this.skiersApi.writeNewLiftRideWithHttpInfo(liftRide, resortId, seasonId, dayId, skierId);

            long endTime = System.nanoTime();
            long executionTimeInMs = (endTime - startTime) / 1000000;

            // Uncomment the following print line for visible output
            // Comment for better client speed
//            System.out.println(Thread.currentThread().getName() + "got response code: " + response.getStatusCode());

            if (latencyRecordQueue != null) {
                // If there is a latencyRecord blocking queue being configured
                // Add the latency record into the queue for part 2 analysis
                LatencyRecord latencyRecord = new LatencyRecord(startTime / 1000000, "POST",
                        executionTimeInMs, response.getStatusCode());
                latencyRecordQueue.offer(latencyRecord);
            }

            int httpStatusCodeCategory = response.getStatusCode() / 100;
            // TODO: Uncomment the following line if want to see the result in progress
            // Comment it since print will severely slow the program
            // System.out.println("Got HTTPS response code: " + response.getStatusCode());
            // Retry for HTTP 4xx or 5xx response codes
            if (httpStatusCodeCategory == 4 || httpStatusCodeCategory == 5) {
                executeRequestWithRetry(times + 1, liftRide, resortId, seasonId, dayId, skierId);
            }
        } catch (ApiException e) {
            // Retry for API exceptions
            executeRequestWithRetry(times + 1, liftRide, resortId, seasonId, dayId, skierId);
        }
        return 1;
    }
}
