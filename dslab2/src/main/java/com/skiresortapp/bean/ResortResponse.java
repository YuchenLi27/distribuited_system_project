package com.skiresortapp.bean;

public class ResortResponse {
//    time here means resort property name
//    private String time;
    private long numSkiers;

    public ResortResponse(long numSkiers) {
//        this.time = time;
        this.numSkiers = numSkiers;
    }

//    public String getTime() {
//        return time;
//    }

    public long getNumSkiers() {
        return numSkiers;
    }
}
