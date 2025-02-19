import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import skiclient.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static skiclient.ClientConfig.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<UploadEvent> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> outputQueue = new LinkedBlockingQueue<>();

        long startTime = System.nanoTime();

        // Start a dedicated thread to generate the random events into the blocking queue
        EventGenerator eventGenerator = new EventGenerator(inputQueue);
        Thread eventGeneratorThread = new Thread(eventGenerator);
        eventGeneratorThread.start();

        // Wait until all events got consumed
        eventGeneratorThread.join();

        // Create 32 threads as clients to upload the events to server
        List<Thread> clientThreads = new ArrayList<>();
        //             i < 1
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            ApiClient apiClient = new ApiClient();
            // TODO: Replace this URL for different server locations like localhost or EC2
            // EC2 server base path
            apiClient.setBasePath(SERVER_BASE_PATH_EC2);

            // localhost server base path
            // apiClient.setBasePath(SERVER_BASE_PATH_LOCALHOST);

            SkiersApi skiersApi = new SkiersApi(apiClient);
            RecordUploadClient recordUploadClient = new RecordUploadClient(skiersApi);
            //                                                                         limit: 10000
            Thread uploaderThread = new RecordUploader(recordUploadClient, inputQueue, 1000, outputQueue);
            clientThreads.add(uploaderThread);
        }

        for (Thread thread : clientThreads) {
            thread.start();
        }

        for (Thread thread : clientThreads) {
            thread.join();
        }

        System.out.println("Completed upload 1000 messages for 32 threads");

        // Create 32 new threads to upload the remaining events
        clientThreads.clear();

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            ApiClient apiClient = new ApiClient();
            // TODO: Replace this URL for different server locations like localhost or EC2
            // EC2 server base path
            apiClient.setBasePath(SERVER_BASE_PATH_EC2);

            // localhost server base path
            // apiClient.setBasePath(SERVER_BASE_PATH_LOCALHOST);

            SkiersApi skiersApi = new SkiersApi(apiClient);
            RecordUploadClient recordUploadClient = new RecordUploadClient(skiersApi);

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

        // Calculate the # of successful and failed requests
        int success = 0;
        int fail = 0;
        for (Integer res : outputQueue) {
            if (res == 1) {
                success++;
            } else {
                fail++;
            }
        }

        System.out.println("Success requests sent out: " + success);
        System.out.println("Fail requests sent out: " + fail);

        // The total run time (wall time) for all phases to complete in seconds
        long executionTime = (endTime - startTime) / (1000000 * 1000);
        System.out.println("Execution time in seconds is: " + executionTime);


        // The total throughput in requests per second
        long throughput = success / executionTime;
        System.out.println("Throughput in requests per seconds is: " + throughput);
    }
}
