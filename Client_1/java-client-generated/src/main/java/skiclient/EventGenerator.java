package skiclient;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class EventGenerator implements Runnable {
    private final Random random;
    private final BlockingQueue<UploadEvent> queue;

    public EventGenerator(BlockingQueue<UploadEvent> queue) {
        this.random = new Random();
        this.queue = queue;
    }

    @Override
    //  client 1 : data generation
    public void run(){
        for (int i = 0; i < 200 * 1000; i++) {
            int randSkierID = random.nextInt(100000) + 1;
            int randResortID = random.nextInt(10) + 1;
            int randliftID = random.nextInt(40) + 1;
            int seasonID = 2025;
            int dayID = 1;
            int time = random.nextInt(360) + 1;

            UploadEvent event = new UploadEvent(randSkierID, randResortID, randliftID,seasonID,dayID, time);
            queue.offer(event);
        }

    }
}
