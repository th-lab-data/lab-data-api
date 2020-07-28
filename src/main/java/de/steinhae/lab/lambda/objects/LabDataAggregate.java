package de.steinhae.lab.lambda.objects;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class LabDataAggregate {

    @SerializedName("lab_id")
    private String labId;

    @SerializedName("avg_temp")
    private double avgTemp;

    @SerializedName("avg_humidity")
    private double avgHumidity;

    private transient long hits;

    public LabDataAggregate(String labId, double avgTemp, double avgHumidity) {
        this.labId = labId;
        this.avgTemp = avgTemp;
        this.avgHumidity = avgHumidity;
    }

    public LabDataAggregate(String labId, double avgTemp, double avgHumidity, long hits) {
        this(labId, avgTemp, avgHumidity);
        this.hits = hits;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public double getAvgTemp() {
        return avgTemp;
    }

    public void setAvgTemp(double avgTemp) {
        this.avgTemp = avgTemp;
    }

    public double getAvgHumidity() {
        return avgHumidity;
    }

    public void setAvgHumidity(double avgHumidity) {
        this.avgHumidity = avgHumidity;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabDataAggregate that = (LabDataAggregate) o;
        return Double.compare(that.avgTemp, avgTemp) == 0 &&
            Double.compare(that.avgHumidity, avgHumidity) == 0 &&
            hits == that.hits &&
            labId.equals(that.labId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labId, avgTemp, avgHumidity, hits);
    }

    @Override
    public String toString() {
        return "LabDataAggregate{" +
            "labId='" + labId + '\'' +
            ", avgTemp=" + avgTemp +
            ", avgHumidity=" + avgHumidity +
            ", hits=" + hits +
            '}';
    }
}
