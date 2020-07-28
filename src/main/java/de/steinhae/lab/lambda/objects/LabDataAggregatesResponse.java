package de.steinhae.lab.lambda.objects;

import java.util.List;
import java.util.Objects;

public class LabDataAggregatesResponse {

    private List<LabDataAggregate> aggregates;
    private Page page;

    public LabDataAggregatesResponse(List<LabDataAggregate> aggregates, Page page) {
        this.aggregates = aggregates;
        this.page = page;
    }

    public List<LabDataAggregate> getAggregates() {
        return aggregates;
    }

    public void setAggregates(List<LabDataAggregate> aggregates) {
        this.aggregates = aggregates;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabDataAggregatesResponse response = (LabDataAggregatesResponse) o;
        return Objects.equals(aggregates, response.aggregates) &&
            Objects.equals(page, response.page);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggregates, page);
    }

    @Override
    public String toString() {
        return "LabDataAggregatesResponse{" +
            "aggregates=" + aggregates +
            ", page=" + page +
            '}';
    }
}
