package skiclient;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static skiclient.ClientConfig.TOTAL_NUMBER_OF_POST;

/**
 * Thread to generate random event to be uploaded to the server
 */
public class EventGenerator implements Runnable {
    private final Random random;
    private final BlockingQueue<UploadEvent> queue;

    public EventGenerator(BlockingQueue<UploadEvent> queue) {
        this.random = new Random();
        this.queue = queue;
    }

    @Override
    public void run() {
        for (int i = 0; i < TOTAL_NUMBER_OF_POST; i++) {
            int randSkierId = random.nextInt(100000) + 1;
            int randResortID = random.nextInt(10) + 1;
            int randLiftID = random.nextInt(40) + 1;
            int randSeasonID = 2024;
            int randDayID = 1;
            int randTime = random.nextInt(360) + 1;

            UploadEvent event = new UploadEvent(randSkierId, randResortID, randLiftID, randSeasonID, randDayID, randTime);

            queue.offer(event);
        }
    }
}