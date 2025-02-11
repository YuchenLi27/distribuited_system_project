package skiersortapp;

public class LiftRide {
    private final int liftID;
    private final int time;

    public LiftRide(int liftID, int time) {
        this.liftID = liftID;
        this.time = time;
    }

    public int getLiftID() {
        return liftID;
    }

    public int getTime() {
        return time;
    }
}
