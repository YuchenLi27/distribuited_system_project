package skiclient;

public class UploadEvent {
    private int skierID;
    private int resortID;
    private int liftID;
    private int seasonID;
    private int dayID;
    private int time;

    public UploadEvent(int skierID, int resortID, int liftID, int seasonID, int dayID, int time) {
        this.skierID = skierID;
        this.resortID = resortID;
        this.liftID = liftID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.time = time;
    }

    public int getSkierID() {
        return skierID;
    }

    public int getResortID() {
        return resortID;
    }

    public int getLiftID() {
        return liftID;
    }

    public int getSeasonID() {
        return seasonID;
    }

    public int getDayID() {
        return dayID;
    }

    public int getTime() {
        return time;
    }

    public void setSkierID(int skierID) {
        this.skierID = skierID;
    }

    public void setResortID(int resortID) {
        this.resortID = resortID;
    }

    public void setLiftID(int liftID) {
        this.liftID = liftID;
    }

    public void setSeasonID(int seasonID) {
        this.seasonID = seasonID;
    }

    public void setDayID(int dayID) {
        this.dayID = dayID;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "UploadEvent{" +
                "skierID=" + skierID +
                ", resortID=" + resortID +
                ", liftID=" + liftID +
                ", seasonID=" + seasonID +
                ", dayID=" + dayID +
                ", time=" + time +
                '}';
    }
}
