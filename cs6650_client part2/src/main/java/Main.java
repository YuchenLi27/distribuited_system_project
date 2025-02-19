import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import skiclient.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static skiclient.ClientConfig.*;

public class Main {
    private static final String CSV_FILE_NAME = "Part2RecordFile.csv";
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        BlockingQueue<UploadEvent> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> outputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<LatencyRecord> latencyRecordQueue = new LinkedBlockingQueue<>();

        long startTime = System.nanoTime();

        // Start a dedicated thread to generate the random events into the blocking queue
        EventGenerator eventGenerator = new EventGenerator(inputQueue);
        Thread eventGeneratorThread = new Thread(eventGenerator);
        eventGeneratorThread.start();

        // Wait until all events got consumed
        eventGeneratorThread.join();

        // Create 32 threads as clients to upload the events to server
        List<Thread> clientThreads = new ArrayList<>();
//        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
//            ApiClient apiClient = new ApiClient();
//            // TODO: Replace this URL for different server locations like localhost or EC2
//            // EC2 server base path
//            apiClient.setBasePath(SERVER_BASE_PATH_EC2);
//
//            // localhost server base path
//            // apiClient.setBasePath(SERVER_BASE_PATH_LOCALHOST);
//            SkiersApi skiersApi = new SkiersApi(apiClient);
//            RecordUploadClient recordUploadClient = new RecordUploadClient(skiersApi);
//
//            // Record latency record for part 2
//            recordUploadClient.setLatencyRecordQueue(latencyRecordQueue);
//
//            Thread uploaderThread = new RecordUploader(recordUploadClient, inputQueue, 1000, outputQueue);
//            clientThreads.add(uploaderThread);
//        }
//
//        for (Thread thread : clientThreads) {
//            thread.start();
//        }
//
//        for (Thread thread : clientThreads) {
//            thread.join();
//        }

        // Create 32 new threads to upload the remaining events
//        clientThreads.clear();
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            ApiClient apiClient = new ApiClient();
            // TODO: Replace this URL for different server locations like localhost or EC2
            // EC2 server base path
            apiClient.setBasePath(SERVER_BASE_PATH_EC2);

            // localhost server base path
            // apiClient.setBasePath(SERVER_BASE_PATH_LOCALHOST);
            SkiersApi skiersApi = new SkiersApi(apiClient);

            // Record latency record for part 2
            RecordUploadClient recordUploadClient = new RecordUploadClient(skiersApi);

            recordUploadClient.setLatencyRecordQueue(latencyRecordQueue);
            // limit -1: let the thread process as much request as they can until the request remaining is 0.
            Thread uploaderThread = new RecordUploader(recordUploadClient, inputQueue, -1, outputQueue);
            clientThreads.add(uploaderThread);
        }

        for (Thread thread : clientThreads) {
            thread.start();
        }

        for (Thread thread : clientThreads) {
            thread.join();
        }

        long endTime = System.nanoTime();
        long executionTimeInSecond = (endTime - startTime) / (1000000 * 1000);

        // Write CSV file
        List<String> dataLines = new ArrayList<>();
        List<LatencyRecord> latencyRecords = new ArrayList<>();
        for (LatencyRecord latencyRecord : latencyRecordQueue) {
            // Copy the latency records from blocking queue to ArrayList for later calculation
            latencyRecords.add(latencyRecord);
            String[] fields = new String[]{
                    String.valueOf(latencyRecord.getStartTime()),
                    String.valueOf(latencyRecord.getRequestType()),
                    String.valueOf(latencyRecord.getLatency()),
                    String.valueOf(latencyRecord.getResponseCode())
            };

            String csvLine = String.join(",", fields);
            dataLines.add(csvLine);
        }

        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.forEach(pw::println);
        }

        // Calculate mean, median, throughput, and P99

        // Calculate mean response time
        long latencySum = 0;
        for (LatencyRecord latencyRecord : latencyRecords) {
            latencySum += latencyRecord.getLatency();
        }

        long mean = latencySum / latencyRecords.size();
        System.out.println("Mean response time in ms is: " + mean);

        // Calculate median response time
        List<Long> latencies = new ArrayList<>();
        latencyRecords.forEach(latencyRecord -> latencies.add(latencyRecord.getLatency()));
        Collections.sort(latencies);
        long median = latencies.get(latencies.size() / 2);
        System.out.println("Median response time in ms is: " + median);

        // Calculate throughput in seconds. We need to divide latencySum by 1000 since it's in milliseconds
        long totalRequests = latencies.size();
        long throughput = totalRequests / executionTimeInSecond;
        System.out.println("Throughput in requests per seconds is: " + throughput);

        // Calculate P99 response time
        int index = (int) Math.ceil(99.0 / 100.0 * latencies.size());
        long p99 = latencies.get(index-1);
        System.out.println("P99 response time in ms is: " + p99);

        // Calculate min and max response time
        long minLatency = latencies.get(0);
        long maxLatency = latencies.get(latencies.size()-1);
        System.out.println("Minimum response in ms time is: " + minLatency);
        System.out.println("Maximum response time in ms is: " + maxLatency);
    }
}
