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

        long first = sensorData.get(0).getTimestamp();
        long last = sensorData.get(sensorData.size() - 1).getTimestamp();

        List<Long> syncTimestamps = new LinkedList<>();
        while (first < last) {
            first = Math.min(first + cutOffInMs, last);
            syncTimestamps.add(first);
        }

        SensorEntry previousDataPoint = new SensorEntry(sensorData.get(0));
        SensorEntry dataPoint = new SensorEntry(sensorData.get(0));
        int pos = 1;
        int count = 1;
        boolean justAdded = false;
        for (long syncTimestamp : syncTimestamps) {
            while (dataPoint.getTimestamp() < syncTimestamp && pos < sensorData.size()) {
                dataPoint = sensorData.get(pos);
                if (dataPoint.getTimestamp() >= syncTimestamp) {
                    previousDataPoint.setTemperature(previousDataPoint.getTemperature() / count);
                    previousDataPoint.setHumidity(previousDataPoint.getHumidity() / count);
                    count = 1;
                    downsampled.add(previousDataPoint);
                    previousDataPoint = dataPoint;
                    justAdded = true;
                } else {
                    previousDataPoint.setTemperature(previousDataPoint.getTemperature() + dataPoint.getTemperature());
                    previousDataPoint.setHumidity(previousDataPoint.getHumidity() + dataPoint.getHumidity());
                    count++;
                    justAdded = false;
                }
                pos++;
            }
            if (pos == sensorData.size() && !justAdded) {
                previousDataPoint.setTemperature(previousDataPoint.getTemperature() / count);
                previousDataPoint.setHumidity(previousDataPoint.getHumidity() / count);
                downsampled.add(previousDataPoint);
                break;
            }
        }
        return downsampled;
    }
}
