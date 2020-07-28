package de.steinhae.lab.lambda;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.google.gson.Gson;

import de.steinhae.lab.lambda.interfaces.BlobStorage;
import de.steinhae.lab.lambda.interfaces.DatabaseStorage;
import de.steinhae.lab.lambda.interfaces.SensorEntryDownsampler;
import de.steinhae.lab.lambda.objects.LabRecord;
import de.steinhae.lab.lambda.objects.SensorEntry;
import de.steinhae.lab.lambda.services.AverageDownsamplingService;
import de.steinhae.lab.lambda.services.AwsS3StorageHelper;
import de.steinhae.lab.lambda.services.ElasticSearchStorageHelper;
import de.steinhae.lab.lambda.services.EnvVarHelper;

public class LabDataConsumer implements RequestHandler<KinesisEvent, Boolean> {

    private static final long DOWNSAMPLE_CUTOFF_IN_SECONDS = 600;
    private static final String LEADING_ZERO_FORMAT = "%02d";
    private static final String JSON_SUFFIX = ".json";

    private DatabaseStorage elasticSearchStorage;
    private BlobStorage awsS3Storage;
    private Gson gson;
    private LambdaLogger logger;
    private SensorEntryDownsampler avgDownsamplingService;
    private String s3Bucket;

    private enum Required {
        ES_HOST,
        ES_PORT,
        ES_SCHEME,
        ES_INDEX_NAME,
        S3_BUCKET
    }

    @Override
    public Boolean handleRequest(KinesisEvent event, Context context) {
        try {
            init(context);
            for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
                String rec = new String(record.getKinesis().getData().array(), StandardCharsets.UTF_8);
                LabRecord labRecord = gson.fromJson(rec, LabRecord.class);
                persistData(labRecord);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (elasticSearchStorage != null) {
                elasticSearchStorage.close();
            }
        }
        return true;
    }

    private void persistData(LabRecord labRecord) {
        if (labRecord != null && labRecord.getData() != null && !labRecord.getData().isEmpty()) {
            logger.log("Processing lab with id " + labRecord.getLabId() + " found " + labRecord.getData().size() + " readings");
            long firstTimestamp = labRecord.getData().get(0).getTimestamp();
            awsS3Storage.writeObjectToCloud(labRecord,
                getS3FilePath(firstTimestamp,
                    String.valueOf(labRecord.getLabId())));
            elasticSearchStorage.saveLabRecord(downsampleLabRecord(labRecord));
        } else {
            StringBuilder sb = new StringBuilder("Skipping invalid or empty record");
            if (labRecord != null) {
                sb.append(" for lab_id: ").append(labRecord.getLabId());
            }
            logger.log(sb.toString());
        }
    }

    private LabRecord downsampleLabRecord(LabRecord labRecord) {
        List<SensorEntry> downsampledData = avgDownsamplingService.downsample(labRecord.getData());
        LabRecord result = new LabRecord();
        result.setData(downsampledData);
        result.setLabId(labRecord.getLabId());
        return result;
    }

    private String getS3FilePath(long firstTimestamp, String labId) {
        DateTime raw = new DateTime(firstTimestamp);
        DateTime dt = raw.withZone(DateTimeZone.UTC);
        int year = dt.getYear();
        String month = String.format(LEADING_ZERO_FORMAT, dt.getMonthOfYear());
        String day = String.format(LEADING_ZERO_FORMAT, dt.getDayOfMonth());
        return s3Bucket + "/" + year + "/" + month + "/" + day + "/" + labId + "_" + firstTimestamp + JSON_SUFFIX;
    }

    private void init(Context ctx) {
        EnvVarHelper.throwExceptionIfEnvVarMissing(Required.values());
        elasticSearchStorage = new ElasticSearchStorageHelper(
            EnvVarHelper.getValue(Required.ES_INDEX_NAME),
            EnvVarHelper.getValue(Required.ES_HOST),
            Integer.parseInt(EnvVarHelper.getValue(Required.ES_PORT)),
            EnvVarHelper.getValue(Required.ES_SCHEME));
        awsS3Storage = new AwsS3StorageHelper();
        s3Bucket = EnvVarHelper.getValue(Required.S3_BUCKET);
        gson = new Gson();
        logger = ctx.getLogger();
        avgDownsamplingService = new AverageDownsamplingService(DOWNSAMPLE_CUTOFF_IN_SECONDS);
    }
}
