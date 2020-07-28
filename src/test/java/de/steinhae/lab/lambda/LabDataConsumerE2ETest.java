package de.steinhae.lab.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetRecordsResult;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.model.Shard;
import com.amazonaws.services.kinesis.model.ShardIteratorType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

public class LabDataConsumerE2ETest {

    private LabDataConsumer handler;
    private AmazonKinesisClient client;
    private String shardIterator;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setUp() {
        initKinesis();
        handler = new LabDataConsumer();
    }

    public void testLambda() {
        environmentVariables.set("ES_HOST", "ELASTIC_SEARCH_DOMAIN");
        environmentVariables.set("ES_INDEX_NAME", "lab-data");
        environmentVariables.set("ES_PORT","443");
        environmentVariables.set("ES_SCHEME", "https");
        environmentVariables.set("S3_BUCKET", "S3_BUCKET");

        Context ctx = Mockito.mock(Context.class);
        Mockito.when(ctx.getLogger()).thenReturn(System.out::println);
        while (true) {
            List<Record> records = readRecord();
            KinesisEvent kinesisEvent = new KinesisEvent();
            KinesisEvent.KinesisEventRecord kinesisEventRecord = new KinesisEvent.KinesisEventRecord();
            List<KinesisEvent.KinesisEventRecord> recordList = new ArrayList<>();
            for (Record record : records) {
                KinesisEvent.Record lambdaRecord = new KinesisEvent.Record();
                lambdaRecord.setData(record.getData());
                kinesisEventRecord.setKinesis(lambdaRecord);
                recordList.add(kinesisEventRecord);
            }
            kinesisEvent.setRecords(recordList);
            handler.handleRequest(kinesisEvent, ctx);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Record> readRecord() {
        GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
        getRecordsRequest.setShardIterator(shardIterator);
        getRecordsRequest.setLimit(1);

        GetRecordsResult recordResult = client.getRecords(getRecordsRequest);
        List<Record> records = recordResult.getRecords();
        shardIterator = recordResult.getNextShardIterator();
        return records;
    }

    public void initKinesis() {
        String amazonStreamName = "KINESIS_STREAM_NAME";

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials("ACCESS_KEY",
            "SECRET_KEY");
        client = new AmazonKinesisClient(awsCredentials);
        client.setRegion(RegionUtils.getRegion("eu-central-1"));

        // Getting initial stream description from aws
        System.out.println(client.describeStream(amazonStreamName).toString());
        List<Shard> initialShardData = client.describeStream(amazonStreamName).getStreamDescription().getShards();
        System.out.println("\nlist of shards:");
        initialShardData.forEach(d -> System.out.println(d.toString()));

        // Getting shardIterators (at beginning sequence number) for reach shard
        List<String> initialShardIterators = initialShardData.stream().map(s ->
            client.getShardIterator(new GetShardIteratorRequest()
                .withStreamName(amazonStreamName)
                .withShardId(s.getShardId())
                .withStartingSequenceNumber(s.getSequenceNumberRange().getStartingSequenceNumber())
                .withShardIteratorType(ShardIteratorType.AT_SEQUENCE_NUMBER)
            ).getShardIterator()
        ).collect(Collectors.toList());

        System.out.println("\nlist of ShardIterators:");
        initialShardIterators.forEach(i -> System.out.println(i));
        System.out.println("\nwaiting for messages....");

        // WARNING!!! Assume that only have one shard. So only use that shard
        shardIterator = initialShardIterators.get(0);
    }
}
