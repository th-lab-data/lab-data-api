package de.steinhae.lab.lambda.services;

import java.util.LinkedList;
import java.util.List;

import de.steinhae.lab.lambda.interfaces.SensorEntryDownsampler;
import de.steinhae.lab.lambda.objects.SensorEntry;

public class AverageDownsamplingService implements SensorEntryDownsampler {

    private long cutOffInMs;

    public AverageDownsamplingService(long cutoffInSeconds) {
        this.cutOffInMs = cutoffInSeconds * 1000;
    }

    @Override
    public List<SensorEntry> downsample(List<SensorEntry> sensorData) {
        List<SensorEntry> downsampled = new LinkedList<>();
        if (sensorData == null) {
            return downsampled;
        } else if (sensorData.size() < 2 || cutOffInMs == 0) {
            return sensorData;
        }

        SensorEntry first = sensorData.get(0);
        SensorEntry avg = new SensorEntry(first);
        long cutOff = first.getTimestamp() + cutOffInMs;
        int avgCount = 1;
        for (int i = 1; i < sensorData.size(); i++) {
            if (sensorData.get(i).getTimestamp() >= cutOff) {
                downsampled.add(calculateAvg(avg, avgCount));
                cutOff += cutOffInMs;
                avgCount = 1;
                avg = sensorData.get(i);
                // there is only one element left that we need to add
                if (i == sensorData.size() - 1) {
                    downsampled.add(avg);
                }
            } else {
                avg.setHumidity(avg.getHumidity() + sensorData.get(i).getHumidity());
                avg.setTemperature(avg.getTemperature() + sensorData.get(i).getTemperature());
                avgCount++;
            }
        }
        if (avgCount > 1) {
            downsampled.add(calculateAvg(avg, avgCount));
        }
        return downsampled;
    }

    private SensorEntry calculateAvg(SensorEntry sum, int count) {
        SensorEntry entry = new SensorEntry(sum);
        entry.setTemperature(entry.getTemperature() / count);
        entry.setHumidity(entry.getHumidity() / count);
        return entry;
    }
}
