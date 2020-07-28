package de.steinhae.lab.lambda.services;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.google.gson.Gson;

import de.steinhae.lab.lambda.interfaces.DatabaseStorage;
import de.steinhae.lab.lambda.objects.LabDataAggregate;
import de.steinhae.lab.lambda.objects.LabRecord;
import de.steinhae.lab.lambda.objects.SensorEntry;

public class ElasticSearchStorageHelper implements DatabaseStorage {

    private static final int DEFAULT_ES_BATCH_SIZE = 480;

    private final String esIndexName;
    private final RestHighLevelClient client;
    private final Gson gson;

    private int esBatchSize = DEFAULT_ES_BATCH_SIZE;

    public ElasticSearchStorageHelper(String esIndexName, String esHost, int esPort, String esScheme) {
        this.esIndexName = esIndexName;
        client = new RestHighLevelClient(
            RestClient.builder(
                new HttpHost(esHost, esPort, esScheme)));
        gson = new Gson();
    }

    public ElasticSearchStorageHelper(String esIndexName, String esHost, int esPort, String esScheme, int esBatchSize) {
        this(esIndexName, esHost, esPort, esScheme);
        this.esBatchSize = esBatchSize;
    }

    @Override
    public void saveLabRecord(LabRecord labRecord) {
        List<BulkRequest> bulkRequests = new LinkedList<>();
        BulkRequest bulkRequest = new BulkRequest();
        int labId = labRecord.getLabId();
        int count = 1;
        for (SensorEntry entry : labRecord.getData()) {
            entry.setLabId(labId);
            IndexRequest request = new IndexRequest(esIndexName);
            String elasticSearchData = gson.toJson(entry);
            String requestId = UUID.nameUUIDFromBytes(elasticSearchData.getBytes()).toString().substring(0, 7);
            request.id(requestId);
            request.source(elasticSearchData, XContentType.JSON);
            bulkRequest.add(request);
            if (count % esBatchSize == 0) {
                bulkRequests.add(bulkRequest);
                bulkRequest = new BulkRequest();
            }
            count += 1;
        }
        if (bulkRequest.numberOfActions() > 0) {
            bulkRequests.add(bulkRequest);
        }
        try {
            for (BulkRequest br : bulkRequests) {
                BulkResponse bulkResponse = client.bulk(br, RequestOptions.DEFAULT);
                if (bulkResponse.hasFailures()) {
                    for (BulkItemResponse bulkItemResponse : bulkResponse) {
                        if (bulkItemResponse.isFailed()) {
                            BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                            throw new RuntimeException(failure.toString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LabDataAggregate> queryAggregationsForLastHours(
        int hours,
        int currentPage,
        int maxPages,
        int maxAmount
    ) {
        List<LabDataAggregate> aggregates;

        IncludeExclude include = new IncludeExclude(currentPage, maxPages);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(getRangeQueryForLastHours(hours));
        TermsAggregationBuilder aggregation = AggregationBuilders.terms("by_lab")
            .field("labId")
            .size(maxAmount)
            .includeExclude(include);
        aggregation.subAggregation(AggregationBuilders.avg("average_temp")
            .field("temp"));
        aggregation.subAggregation(AggregationBuilders.avg("average_humidity")
            .field("humidity"));
        searchSourceBuilder.aggregation(aggregation);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(esIndexName);
        searchRequest.source(searchSourceBuilder);

        List<? extends Terms.Bucket> buckets;
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            buckets = ((ParsedLongTerms) response.getAggregations().get("by_lab")).getBuckets();
            aggregates = buckets.stream()
                .map(bucket -> {
                    double avgTemp = getAggregationValue(bucket, "average_temp");
                    double avgHumidity = getAggregationValue(bucket, "average_humidity");
                    String labId = String.valueOf(bucket.getKey());
                    return new LabDataAggregate(labId, avgTemp, avgHumidity, bucket.getDocCount());
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return aggregates;
    }

    private RangeQueryBuilder getRangeQueryForLastHours(int hours) {
        DateTime now = DateTime.now().toDateTime(org.joda.time.DateTimeZone.UTC);
        return QueryBuilders.rangeQuery("datetime")
            .gte(now.minusHours(hours)).lt(now);
    }

    private double getAggregationValue(Terms.Bucket bucket, String name) {
        return ((ParsedAvg) bucket.getAggregations().get(name)).getValue();
    }

    @Override
    public long queryTotalLabsForLastHours(int hours) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(getRangeQueryForLastHours(hours));
        CardinalityAggregationBuilder aggregation = AggregationBuilders.cardinality("type_count")
            .field("labId");
        searchSourceBuilder.aggregation(aggregation);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(esIndexName);
        searchRequest.source(searchSourceBuilder);

        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            return ((ParsedCardinality) response.getAggregations().get("type_count")).getValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
