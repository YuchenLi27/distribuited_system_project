package skiersortapp;

import java.util.ArrayList;
import java.util.List;

public class SkierVerticalRecords {
    private List<SkierVerticalRecord> resorts = new ArrayList<>();

    public SkierVerticalRecords(List<SkierVerticalRecord> resorts) {
        this.resorts = resorts;
    }

    public List<SkierVerticalRecord> getResorts() {
        return resorts;
    }

    public void setResorts(List<SkierVerticalRecord> resorts) {
        this.resorts = resorts;
    }

    public void addResort(SkierVerticalRecord resort) {
        this.resorts.add(resort);
    }
}
