import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import skiclient.EventGenerator;
import skiclient.RecordUploader;
import skiclient.RecordUploadClient;
import skiclient.UploadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static <list> void main(String[] args) throws InterruptedException {
        BlockingQueue<UploadEvent> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> outputQueue = new LinkedBlockingQueue<>();
        long startTime = System.nanoTime();

        EventGenerator eventGenerator = new EventGenerator(inputQueue);
        Thread eventGeneratorThread = new Thread(eventGenerator);
        eventGeneratorThread.start();

        eventGeneratorThread.join();

        List<Thread> clientThreads = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            ApiClient apiClient = new ApiClient();
            // TODO: Replace this URL for server locations  EC2
            apiClient.setBasePath("http://54.174.248.221:8080/dslab2_war_exploded/skiers/12/seasons/2019/day/1/skier/123");
            SkiersApi skiersApi = new SkiersApi(apiClient);
            RecordUploadClient recordUploadClient = new RecordUploadClient(skiersApi);

            Thread uploderThread = new RecordUploader(recordUploadClient, inputQueue,1000, outputQueue);
            clientThreads.add(uploderThread);
        }

        for (Thread thread : clientThreads) {
            thread.start();
            thread.join();
        }
        long endTime = System.nanoTime();
        int success = 0;
        int fail = 0;
        for (Integer res: outputQueue) {
            if (res == 1) {
                success++;
            } else{
                fail++;
            }
        }
        System.out.println("Success request sent out " + success);
        System.out.println("Fail request sent out " + fail);
        long elapsedTime = (endTime - startTime) /1000000 * 1000;
        System.out.println("Elapsed time: " + elapsedTime);
        long throughput= success/elapsedTime;
        System.out.println("Throughput in request per seconds is: " + throughput);


    }


}

