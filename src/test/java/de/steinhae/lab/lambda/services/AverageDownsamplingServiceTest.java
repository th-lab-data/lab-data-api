package de.steinhae.lab.lambda.services;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.steinhae.lab.lambda.objects.SensorEntry;

public class AverageDownsamplingServiceTest {

    public static final int EXPECTED_TEMPERATURE = 20;
    public static final double EXPECTED_HUMIDITY = 56.1;
    public static final double ALLOWED_DELTA = 0.1;
    public static final long START_TIMESTAMP = 1595718000000L;
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
        long startTimestamp = START_TIMESTAMP;
        List<Long> expectedTimestamps = new LinkedList<>();
        for (int i = 0; i < 2880; i++) {
            input.add(new SensorEntry(EXPECTED_TEMPERATURE, EXPECTED_HUMIDITY, startTimestamp));
            if (i % 20 == 0) {
                expectedTimestamps.add(startTimestamp);
            }
            startTimestamp += 30000;
        }
        List<SensorEntry> actual = service.downsample(input);
        Assert.assertEquals(144, actual.size());
        for (SensorEntry entry : actual) {
            Assert.assertEquals(EXPECTED_HUMIDITY, entry.getHumidity(), ALLOWED_DELTA);
            Assert.assertEquals(EXPECTED_TEMPERATURE, entry.getTemperature(), ALLOWED_DELTA);
            Assert.assertEquals(expectedTimestamps.remove(0), entry.getTimestamp(), ALLOWED_DELTA);
        }
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

    @Test
    public void testOneElementOverCutoff() {
        List<SensorEntry> input = new LinkedList<>();
        input.add(new SensorEntry(2, 4, 1));
        input.add(new SensorEntry(4, 6, 100));
        input.add(new SensorEntry(7, 7, 200));
        SensorEntry expectedFirst = calculateAverageValues(input);

        SensorEntry expectedSecond = new SensorEntry(20, 50, 600001);
        input.add(expectedSecond);

        List<SensorEntry> actual = service.downsample(input);
        Assert.assertEquals(2, actual.size());
        Assert.assertEquals(expectedFirst, actual.get(0));
        Assert.assertEquals(expectedSecond, actual.get(1));

    }

    @Test
    public void testElementsHighCutoff() {
        List<SensorEntry> input = new LinkedList<>();
        input.add(new SensorEntry(2, 4, 1));
        testAverages(input);

        input.add(new SensorEntry(4, 6, 100));
        testAverages(input);

        input.add(new SensorEntry(7, 7, 200));
        testAverages(input);
    }

    private void testAverages(List<SensorEntry> input) {
        List<SensorEntry> expected = Collections.singletonList(calculateAverageValues(input));
        Assert.assertEquals(expected, service.downsample(input));
    }

    private SensorEntry calculateAverageValues(List<SensorEntry> input) {
        double temp = 0;
        double hum = 0;
        int count = 0;
        for (SensorEntry entry : input) {
            temp += entry.getTemperature();
            hum += entry.getHumidity();
            count++;
        }
        return new SensorEntry(temp / count, hum / count, input.get(0).getTimestamp());
    }

}
