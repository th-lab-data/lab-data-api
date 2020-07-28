package de.steinhae.lab.lambda.objects;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class LabRecord {

    @SerializedName("lab_id")
    private int labId;
    private List<SensorEntry> data;

    public int getLabId() {
        return labId;
    }

    public void setLabId(int labId) {
        this.labId = labId;
    }

    public List<SensorEntry> getData() {
        return data;
    }

    public void setData(List<SensorEntry> data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabRecord labRecord = (LabRecord) o;
        return labId == labRecord.labId &&
            Objects.equals(data, labRecord.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labId, data);
    }

    @Override
    public String toString() {
        return "LabRecord{" +
            "labId=" + labId +
            ", data=" + data +
            '}';
    }
}
