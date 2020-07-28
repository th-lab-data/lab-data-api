package de.steinhae.lab.lambda.interfaces;

import java.util.List;

import de.steinhae.lab.lambda.objects.LabDataAggregate;
import de.steinhae.lab.lambda.objects.LabRecord;

public interface DatabaseStorage {
    void saveLabRecord(LabRecord labRecord);

    List<LabDataAggregate> queryAggregationsForLastHours(int hours, int currentPage, int maxPages, int maxAmount);

    long queryTotalLabsForLastHours(int hours);

    void close();
}
