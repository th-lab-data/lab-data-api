package de.steinhae.lab.lambda.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RequestStreamUtils {

    public static int parseQueryStringParameter(InputStream inputStream, String parameter) {
        JsonParser parser = new JsonParser();;
        int result = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JsonObject event = parser.parse(reader).getAsJsonObject();
        if (event.has("queryStringParameters")) {
            JsonObject params = event.get("queryStringParameters").getAsJsonObject();
            if (params.has(parameter)) {
                result = params.get(parameter).getAsInt();
            }
        }
        return result;
    }
}
