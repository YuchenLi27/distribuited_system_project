package skiersortapp;

public class SkierVerticalRecord {
    //this class servers the GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
    //it responses with "total vertical for the day returned"
    private String seasonID;
    private int totalVert;


    public SkierVerticalRecord(String seasonID, int totalVert) {
        this.seasonID = seasonID;
        this.totalVert = totalVert;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public int getTotalVert() {
        return totalVert;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public void setTotalVert(int totalVert) {
        this.totalVert = totalVert;
    }

}
