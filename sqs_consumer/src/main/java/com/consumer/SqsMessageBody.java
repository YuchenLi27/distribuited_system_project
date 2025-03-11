package com.consumer;

public class SqsMessageBody {
    private int resortId;
    private String seasonId;
    private String dayId;
    private int skierId;
    private LiftRide liftRide;

    public SqsMessageBody(int resortId, String seasonId, String dayId, int skierId, LiftRide liftRide) {
        this.resortId = resortId;
        this.seasonId = seasonId;
        this.dayId = dayId;
        this.skierId = skierId;
        this.liftRide = liftRide;

    }

}
