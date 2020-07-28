package de.steinhae.lab.lambda.services;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.steinhae.lab.lambda.objects.SensorEntry;

public class AverageDownsamplingServiceTest {

    private AverageDownsamplingService service;

    @Before
    public void setUp() {
        int cutoff = 600;
        service = new AverageDownsamplingService(cutoff);
    }

    @Test
    public void testNullInput() {
        Assert.assertEquals(new LinkedList<>(), service.downsample(null));
    }

    @Test
    public void testLessThanTwoElements() {
        List<SensorEntry> input = new LinkedList<>();
        input.add(new SensorEntry(1, 2, 3));
        Assert.assertEquals(input, service.downsample(input));
    }

    @Test
    public void test2880Elements() {
        List<SensorEntry> input = new LinkedList<>();
        long startTimestamp = 1595718000000L;
        for (int i = 0; i < 2880; i++) {
            input.add(new SensorEntry(2, 2, startTimestamp));
            startTimestamp += 30000;
        }
        List<SensorEntry> actual = service.downsample(input);
        Assert.assertEquals(144, actual.size());
    }

    @Test
    public void testZeroCutoff() {
        AverageDownsamplingService service = new AverageDownsamplingService(0);
        List<SensorEntry> input = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            input.add(new SensorEntry(1, 2, 3));
        }
        Assert.assertEquals(input, service.downsample(input));
    }

}
