package de.steinhae.lab.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.Headers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.steinhae.lab.lambda.interfaces.DatabaseStorage;
import de.steinhae.lab.lambda.objects.LabDataAggregate;
import de.steinhae.lab.lambda.objects.LabDataAggregatesResponse;
import de.steinhae.lab.lambda.objects.Page;
import de.steinhae.lab.lambda.services.ElasticSearchStorageHelper;
import de.steinhae.lab.lambda.services.EnvVarHelper;
import de.steinhae.lab.lambda.services.RequestStreamUtils;

public class LabDataAggregates implements RequestStreamHandler {

    private static final int ELASTIC_MAX_PAGE_SIZE = 5000;
    private static final int QUERY_DATA_FOR_HOURS = 24;

    private final Gson gson = new Gson();
    private LambdaLogger logger;

    private enum Required {
        ES_HOST,
        ES_PORT,
        ES_SCHEME,
        ES_INDEX_NAME
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        EnvVarHelper.throwExceptionIfEnvVarMissing(Required.values());
        logger = context.getLogger();
        JsonObject responseJson = new JsonObject();
        DatabaseStorage elasticSearchStorage = null;

        try {
            elasticSearchStorage = new ElasticSearchStorageHelper(
                EnvVarHelper.getValue(Required.ES_INDEX_NAME),
                EnvVarHelper.getValue(Required.ES_HOST),
                Integer.parseInt(EnvVarHelper.getValue(Required.ES_PORT)),
                EnvVarHelper.getValue(Required.ES_SCHEME));

            double totalLabs = elasticSearchStorage.queryTotalLabsForLastHours(QUERY_DATA_FOR_HOURS);
            int totalPages = (int) (Math.ceil(totalLabs / ELASTIC_MAX_PAGE_SIZE));
            int currentPage = parsePage(inputStream, totalPages);

            List<LabDataAggregate> aggregates = elasticSearchStorage.queryAggregationsForLastHours(QUERY_DATA_FOR_HOURS,
                currentPage,
                totalPages,
                ELASTIC_MAX_PAGE_SIZE);

            Page page = new Page(currentPage + 1, aggregates.size(), (int) totalLabs, 1, totalPages);
            LabDataAggregatesResponse response = new LabDataAggregatesResponse(aggregates, page);

            JsonObject headerJson = new JsonObject();
            headerJson.addProperty(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            responseJson.add("headers", headerJson);
            responseJson.addProperty("statusCode", HttpStatus.SC_OK);
            responseJson.addProperty("body", gson.toJson(response));
        } catch (Exception e) {
            responseJson.addProperty("statusCode", HttpStatus.SC_BAD_REQUEST);
            responseJson.addProperty("exception", ExceptionUtils.getStackTrace(e));
        } finally {
            if (elasticSearchStorage != null) {
                elasticSearchStorage.close();
            }
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write(responseJson.toString());
        writer.close();
    }

    private int validateInt(int value, int minValue, int maxValue) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    private int parsePage(InputStream inputStream, int maxPage) {
        int page = 1;
        try {
            page = RequestStreamUtils.parseQueryStringParameter(inputStream, "page");
        } catch (Exception e) {
            logger.log("Error while parsing input parameter page.");
        }
        return validateInt(page - 1, 0, maxPage - 1);
    }
}
