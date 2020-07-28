package de.steinhae.lab.lambda.objects;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class SensorEntry {

    @SerializedName("temp")
    private double temperature;

    private int labId;
    private double humidity;
    private long timestamp;

    public SensorEntry(double temperature, double humidity, long timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }

    public SensorEntry(SensorEntry entry) {
        this.timestamp = entry.getTimestamp();
        this.humidity = entry.getHumidity();
        this.temperature = entry.getTemperature();
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLabId() {
        return labId;
    }

    public void setLabId(int labId) {
        this.labId = labId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorEntry that = (SensorEntry) o;
        return Double.compare(that.temperature, temperature) == 0 &&
            labId == that.labId &&
            Double.compare(that.humidity, humidity) == 0 &&
            timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(temperature, labId, humidity, timestamp);
    }

    @Override
    public String toString() {
        return "SensorEntry{" +
            "temperature=" + temperature +
            ", labId=" + labId +
            ", humidity=" + humidity +
            ", timestamp=" + timestamp +
            '}';
    }
}
