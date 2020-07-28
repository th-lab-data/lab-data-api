package de.steinhae.lab.lambda.interfaces;

import java.util.List;

import de.steinhae.lab.lambda.objects.SensorEntry;

public interface SensorEntryDownsampler {
    List<SensorEntry> downsample(List<SensorEntry> data);
}
